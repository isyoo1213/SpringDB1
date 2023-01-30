package springDB.jdbc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.MemberRepositoryV2;
import springDB.jdbc.repository.MemberRepositoryV3;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * PlatformTransactionManager 적용
 * Service 계층에 침투한 구체화된 의존관계의 제거
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_1 {

    // * 이제 Service는 dataSource를 통한 getConnection()으로 transaction에 사용할 con을 생성하고 넘겨주지 않아도 됨
    // -> dataSource에 의존하지 않음 + transaction을 시작/종료할 PlatformTransactionManager에 의존
    //private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    //JDBC 관련 구현체인 DataSourceTransactionManager를 주입받을 예정 cf) JpaTransactionManager

    private final MemberRepositoryV3 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        // 더이상 트랜잭션에 사용할 conn을 직접 생성해주지 않음
        // *** con의 제어권은 repository의 getConnection()을 담당하는 DataSourceUtils에서 con의 관리여부 파악에 따라 생성 제어됨
        // + cf) 소멸 제어
        // 1. '동기화'를 위한 비즈니스 로직 단계 - repository의 DataSourceUtils
        // 2. commit이나 rollback 후 Service 단계 - transactionManager의 내부 처리
        //Connection con = dataSource.getConnection();

        // * Transaction 시작 by transactionManager의 getTransaction()
        // * DefaultTransactionDefinition - transaction을 위한 기본적인 상수들이 설정되어 있음
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        // * TransactionStatus - 현재 Transaction의 상태 정보가 포함되어 있음 -> 이후 commit/rollback에 필요

        try {
            //기존 transaction 시작을 위한 set
            //con.setAutoCommit(false);

            // * 비즈니스로직 실행
            bizLogic(fromId, toId, money);

            // * 성공시 - commit
            transactionManager.commit(status);

        } catch (Exception e) {
            //* 실패시 - RollBack
            transactionManager.rollback(status);
            throw new IllegalStateException(e); //기존 예외를 한 번 감싸서
        }
        // *** 이제 더이상 con의 '동기화' 소멸 제어에 관여하지 않음 + 'commit/rollbakc' 이후 소멸 제어의 자동화
        // -> commit or rollback 발생 -> 필연적인 transaction과 conn의 종료를 의미 -> transactionManager 내부에서 이를 위한 로직 처리
        // -> *** Service 계층에서는 더이상 con의 관리에 신경쓰거나 의존하지 않고 비즈니스 로직에 집중

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
