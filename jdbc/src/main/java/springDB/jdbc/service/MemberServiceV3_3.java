package springDB.jdbc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.MemberRepositoryV3;

import java.sql.SQLException;

/**
 * @Transactional AOP 적용
 */
@Slf4j
public class MemberServiceV3_3 {

    // * 기존의 transaction 관련 의존 관계 제거
    //private final TransactionTemplate txTemplate;
    private final MemberRepositoryV3 memberRepository;

    public MemberServiceV3_3(MemberRepositoryV3 memberRepository) {
        //this.txTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    // 이제 비즈니스 관련 로직만 남김
    @Transactional
    // 어노테이션이 적용된 메서드 실행에 transaction을 적용하겠다
    // + 클래스 수준에서도 사용 가능 -> 외부에서 호출가능한 모든 'public' 메서드가 AOP 적용 대상이 됨
    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        bizLogic(fromId, toId, money);
    }

    private void bizLogic(String fromId, String toId, int money) throws SQLException {
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
