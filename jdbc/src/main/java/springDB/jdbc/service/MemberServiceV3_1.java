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
        // *** '동기화된' con의 제어권
        // - repository의 getConnection()을 담당하는 DataSourceUtils - '트랜잭션 동기화 매니저'
        // - con의 관리여부 파악에 따라 생성 제어됨
        // cf) 소멸 제어
        // 1. '동기화'를 위한 비즈니스 로직 단계 - repository의 DataSourceUtils - 트랜잭션 동기화 매니저
        // 2. commit이나 rollback 후 Service 단계 - transactionManager의 내부 처리 - 트랜잭션 매니저
        //Connection con = dataSource.getConnection();

        // * 실질적인 동작 순서
        // Transaction Start
        // 1. transaction manager에서 dataSource를 활용한 conn 획득
        // 2. 수동 모드 commit을 통해 실제 DB의 transaction 시작
        // 3. conn을 transaction sync manager에 저장 - threadLocal에 저장 -> Multi Thread 환경에서 안전하게 보관 가능
        // Transaction Logic
        // 1. bizLogic의 DB 접근 호출 - repository
        // 2. DataSourceUtils.getConnection(dataSource)를 통해 '트랜잭션 동기화 매니저'에 보관된 '동기화된' conn 조회 및 사용
        // Transaction End - '동기화된' conn의 경우
        // - bizLogic인 repository 계층에서 close()하지 않고, Service 계층의 Transaction 종료 시점에 close() 해주어야함
        // - by transaction manager의 commit or rollback
        // 1. From transaction sync manager에서 종료시킬 '동기화된' conn을 조회 To transaction manager
        // 2. 해당 conn을 통해 transaction을 commit or rollback - transaction manager, 즉 Service 계층
        // 3. 전체 resource 정리
        // - transaction sync manager 정리 -> threadLocal의 정리
        // - con.setAutoCommit(true) 로 되돌림 -> For Connection Pool로 반환되는 con의 상태를 위해
        // - con.close() 호출 -> 종료 or 반환

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
