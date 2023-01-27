package springDB.jdbc.service;

import lombok.RequiredArgsConstructor;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.MemberRepositoryV1;

import java.sql.SQLException;

@RequiredArgsConstructor //final만으로 생성자 주입 가능 -> 생략하고 생성자 추가해주는 것으로 대체 가능
public class MemberServiceV1 {

    private final MemberRepositoryV1 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {

        // * Transaction의 원자성을 위해 Service의 메서드 단위로 트랜잭션 시작
        // -> Connection이나 SQL과 직접적으로 맞대는 Low-Level 클래스보다 원자성 보장이 쉽고 논리적
        // -> 결국 Service 계층에서 Connection을 획득하는 메서드와 함께 * 동일한 conn 내에서 트랜잭션 처리
        // -> * V2에서는 conn을 Parameter로 넘겨 동일한 conn을 유지하는 방식을 사용

        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        // 상황 - 계좌1에서의 인출 성공 + 계좌2 검증에서 오류발생 -> 계좌2의 입금까지 로직이 이어지지 않음
        memberRepository.update(fromId, fromMember.getMoney() - money);

        //Ctrl + Alt + M 메서드로 뽑아내기
        validation(toMember);

        memberRepository.update(toId, toMember.getMoney() + money);

        // * Commit or RollBack
    }
    private void validation(Member toMember) {
        //예시를 위한 고의적인 예외 발생
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
