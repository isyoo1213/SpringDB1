package springDB.jdbc.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.ex.MyDbException;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * 인터페이스인 SQLExceptionTranslator 적용하기
 */

@Slf4j
public class MemberRepositoryV4_2 implements MemberRepository{
    //Repository 인터페이스 구현 -> @Override 표시해주는 것이 좋음

    //의존 관계 주입 - 생성자 주입
    private final DataSource dataSource;

    // * SQLExceptionTranslator - 인터페이스
    // - ErrorCode 외의 다른 방식으로 Exception을 translate하는 구현체 제공
    private final SQLExceptionTranslator exTranslator;

    public MemberRepositoryV4_2(DataSource dataSource) {
        this.dataSource = dataSource;

        // 생성 시 dataSource를 담아주면, 어떤 DB를 사용하는지 등등의 dataSource의 설정값들을 활용함
        this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
    }

    @Override
    public Member save(Member member) {
        String sql = "insert into member(member_id, money) values(?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());

            pstmt.executeUpdate();

            return member;
        } catch (SQLException e) { //오류 발생 시 로그 정도만 확인하고 예외를 던지도록 구성
            log.error("db error = {}", e);

            // * 이제 직접 Exception을 throw하지 않고, translator에 담아줌
            // + translate() 메서드 자체는 DataAccessException의 자식 Exception들을 반환
            // ->> 이제 방대한 예외계층을 이 translator가 DB에서 전해진 errorCode에 맞춰서 throw해줌
            DataAccessException ex = exTranslator.translate("save", sql, e);
            throw ex;
            //throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }
    }

    @Override
    public Member findById(String memberId) {
        String sql = "select * from member where member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, memberId);

            rs = pstmt.executeQuery();
            if (rs.next()) {
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            } else {
                throw new NoSuchElementException("member not found. memberId = " + memberId);
            }
        } catch (SQLException e) {
            log.error("db error = {}", e);

            DataAccessException ex = exTranslator.translate("findById", sql, e);
            throw ex;
            //throw new MyDbException(e);
        } finally {
            close(con, pstmt, rs);
        }
    }

    @Override
    public void update(String memberId, int money) {
        String sql = "update member set money=? where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);

            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        } catch (SQLException e) {
            log.error("db error", e);

            DataAccessException ex = exTranslator.translate("update", sql, e);
            throw ex;

            //throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }

    }

    @Override
    public void delete(String memberId) {
        String sql = "delete from member where member_id=?";

        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, memberId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("db error", e);

            DataAccessException ex = exTranslator.translate("delete", sql, e);
            throw ex;

            //throw new MyDbException(e);
        } finally {
            close(con, pstmt, null);
        }

    }

    // *DataSourceUtils 사용
    private void close(Connection con, Statement stmt, ResultSet rs) {

        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);

        // * release() 또한 dataSource를 넘겨주는 것이 다른 점
        // *** Transaction을 사용하기 위해 동기화된 con이 존재 시, close()하지 않고 유지해준다
        // + TransactionSynchronizationManager이 고나리하는 con이 없을 경우, close() 진행
        // 즉, Service 계층에서 가지고있던 close()의 제어권을
        // -> DataSourceUtils의 TransactionSynchronizationManager가 con 관리여부를 파악한 뒤 close()의 제어권 행사
        DataSourceUtils.releaseConnection(con, dataSource);
        //JdbcUtils.closeConnection(con);
    }

    // * DataSourceUtils를 통한 Transaction 동기화 사용
    private Connection getConnection() throws SQLException {

        // *** getConnection() 메서드 내의 doGetConnection() 메서드의 TransactionSynchronizationManager가 connection 관리
        // 관리 중인 con이 존재할 경우, getResource()를 통해 기존 con을 가져오는 구조 or 없을 경우에는 새로 con을 생성해서 반환
        Connection con = DataSourceUtils.getConnection(dataSource);
        //Connection con = dataSource.getConnection();

        log.info("get connection = {}, class = {}", con, con.getClass());
        return con;
    }

}
