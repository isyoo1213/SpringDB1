package springDB.jdbc.exception.translator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcUtils;
import springDB.jdbc.connection.ConnectionConst;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.ex.MyDbException;
import springDB.jdbc.repository.ex.MyDuplicateKeyException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import static springDB.jdbc.connection.ConnectionConst.*;

@Slf4j
class ExTranslatorV1Test {

    Repository repository;
    Service service;

    @BeforeEach
    void init() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        repository = new Repository(dataSource);
        service = new Service(repository);
    }

    @Test
    void duplicateKeySave() {
        service.create("myId");
        service.create("myId"); //같은 ID 저장 시도
        // * 테스트 실행 후 h2에서 확인하기
    }

    @RequiredArgsConstructor
    static class Service {
        private final Repository repository;

        public void create(String memberId) {

            try {
                // * Service에서 memberId를 받아 새로운 회원을 저장 + 오류 시 새로운 key를 만들어 대응하는 상황 가정
                repository.save(new Member(memberId, 0));

                log.info("saveId = {}", memberId);
            } catch (MyDuplicateKeyException e) {
                log.info("키 중복, 복구 시도");

                String retryId = generateNewId(memberId);
                log.info("retryId = {}", retryId);

                repository.save(new Member(retryId, 0));
            } catch (MyDbException e) { //키 중복 예외 이외의 DB 예외 계층을 구성해 보는 정도
                log.info("데이터 접근 계층 예외 발생", e);
                throw e;
            }
        }

        private String generateNewId(String memberId) {
            return memberId + new Random().nextInt(10000);
        }
    }

    @RequiredArgsConstructor
    static class Repository {
        private final DataSource dataSource;

        public Member save(Member member) {

            String sql = "insert into member(member_id, money) values(?,?)";

            Connection con = null;
            PreparedStatement pstmt = null;

            try {
                con = dataSource.getConnection();
                pstmt = con.prepareStatement(sql);
                pstmt.setString(1, member.getMemberId());
                pstmt.setInt(2, member.getMoney());
                pstmt.executeUpdate();

                return member;
            } catch (SQLException e) {
                // * h2 DB일 경우 errorCode 확인하기
                if (e.getErrorCode() == 23505) {
                    throw new MyDuplicateKeyException(e);
                    // 이렇게 Repository에서는 SQLException 을 그대로 Service에 넘기는 것이 아닌, 오류코드에 따른 사용자 예외로 던지기
                }
                throw new MyDbException();
            } finally {
                JdbcUtils.closeStatement(pstmt);
                JdbcUtils.closeConnection(con);
            }

        }

    }

}
