package springDB.jdbc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.MemberRepository;

/**
 * 예외 누수 문제 해결
 * MemberRepository 인터페이스에 의존
 * SQLException 제거
 * -> Transaction AOP, Logging을 제외하면 이제 거의 순수한 JAVA 코드로 Service 계층을 구성가능하게 됨
 */
@Slf4j
public class MemberServiceV4 {

    // * 이제 interface에 의존
    //private final MemberRepositoryV3 memberRepository;
    private final MemberRepository memberRepository;

    // * 주입 또한 interface
    public MemberServiceV4(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public void accountTransfer(String fromId, String toId, int money) {
        bizLogic(fromId, toId, money);
    }

    // * interface에 의존하면서 SQLException 생략 가능해짐
    private void bizLogic(String fromId, String toId, int money) {
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney() + money);
    }

    private void validation(Member toMember) {
        //예시를 위한 고의적인 예외 발생
        if (toMember.getMemberId().equals("ex")) {
            log.info("validation error");
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
