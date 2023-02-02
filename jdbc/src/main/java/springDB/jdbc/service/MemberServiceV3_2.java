package springDB.jdbc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.MemberRepositoryV3;

import java.sql.SQLException;

/**
 * Transaction Template 적용
 * 1. transaction 시작
 * 2. bizLogic 실행
 * 3. commit or rollback 수행 후 transaction 종료
 */
@Slf4j
//@RequiredArgsConstructor //이제 생성자에서 로직이 필요하므로 해당 어노테이션 제거
public class MemberServiceV3_2 {


    // * 기존의 PlatformTransactionManager 주입을 직접받는 것이 아닌, TransactionTemplate 생성 과정에서 주입
    //private final PlatformTransactionManager transactionManager;
    private final TransactionTemplate txTemplate;
    private final MemberRepositoryV3 memberRepository;

    // *** 생성자에서 외부 설정으로 bean등록된 '클래스' TransactionTemplate를 주입하는 방식이 아님 -> 클래스는 유연성이 없음
    // -> PlatformTransactionManager를 parameter로 받아, 생성자에서 template 생성에 사용하는 패턴
    // + PlatformTransactionManager는 추상화된 인터페이스임을 잊지 말 것 - 이후 DB에 따른 구체화 클래스로 유연하게 활용 가능
    public MemberServiceV3_2(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository) {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {

        // * 1. transaction 시작
        // * TransactionTemplate은 transactionManager를 가지고 있기 때문에, 기능 활용 가능
        // * status - 기존 transactionManager에서 DefaultTransactionDefinition()을 넘겨 getTransaction()해온 상태 정보
        // -> 이를 활용해 람다식으로 로직 구성
        txTemplate.executeWithoutResult((status)->{

            // * 2. 비즈니스 로직 실행
            // - 해당 람다에서는 bizLogic()이 던지는 SQLException을 잡아줄 수 없으므로, try-catch로 잡아주어야 함
            try {
                bizLogic(fromId, toId, money);
            } catch (SQLException e) {
                // * 기존처럼 checked Exception을 RuntimeException으로 바꿔서 던져야함
                // * Checked Exception 발생 -> Commit이 호출되어버림
                // cf) Unchecked Exception 발생 -> RollBack / 이는 뒷부분에서 다룰 예정

                throw new IllegalStateException(e);
            }

            // * 3. commit or rollback 후 transaction 종료
            // -> 이는 template 내부적으로 모두 처리해줌
        });

    /*
    //transactionManager를 활용한 기존의 반복되던 구조
        // * Transaction 시작
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

        try {
            // * 비즈니스로직 실행
            bizLogic(fromId, toId, money);

            // * 성공시 - commit
            transactionManager.commit(status);

        } catch (Exception e) {
            //* 실패시 - RollBack
            transactionManager.rollback(status);
            throw new IllegalStateException(e); //기존 예외를 한 번 감싸서
        }
    */

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
