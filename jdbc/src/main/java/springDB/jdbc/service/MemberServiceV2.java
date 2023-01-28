package springDB.jdbc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.MemberRepositoryV1;
import springDB.jdbc.repository.MemberRepositoryV2;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Transaction 적용
 * 1. Parameter 연동
 * 2. Pool을 고려한 종료
 */
@Slf4j
@RequiredArgsConstructor //final만으로 생성자 주입 가능 -> 생략하고 생성자 추가해주는 것으로 대체 가능
public class MemberServiceV2 {

    // * Transaction에 사용할 Connection 획득을 위한 DataSource 의존
    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        // 트랜잭션에 사용할 conn 획득
        Connection con = dataSource.getConnection();

        try {
            // *** Transaction 시작 by Conn을 통한 DB에 CommitMode 설정
            con.setAutoCommit(false);

            // * Transaction을 위한 Conn 설정과 CommitMode 설정 후, 비즈니스로직 실행
            // + *** 이 외의 코드들은 모두 Transaction을 처리하기 위한 코드  -> 한 메서드 내에서 분리
            bizLogic(con, fromId, toId, money);

            // * 성공시 - 정상 수행 후 conn을 통해 DB에 commit 실행
            con.commit();

        } catch (Exception e) {
            //* 실패시 - RollBack + Throwing
            con.rollback();
            throw new IllegalStateException(e); //기존 예외를 한 번 감싸서
        } finally {
            // * 성공 or 실패 처리 후 Resource release
            release(con);
        }

    }

    private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException {
        Member fromMember = memberRepository.findById(con, fromId);
        Member toMember = memberRepository.findById(con, toId);

        memberRepository.update(con, fromId, fromMember.getMoney() - money);
        validation(toMember);
        memberRepository.update(con, toId, toMember.getMoney() + money);
    }

    private void release(Connection con) {
        if (con != null) {
            try {
                // *** conn을 close()시 Pool로 반환함
                // -> setAutoCommit() 설정이 false인채로 반환되므로, 세팅해주어야 함
                con.setAutoCommit(true);

                con.close();
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }

    private void validation(Member toMember) {
        //예시를 위한 고의적인 예외 발생
        if (toMember.getMemberId().equals("ex")) {
            log.info("validation error");
            throw new IllegalStateException("이체중 예외 발생");
        }
    }
}
