package springDB.jdbc.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import springDB.jdbc.domain.Member;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/**
 * JDBCTemplate 사용하기
 */

@Slf4j
public class MemberRepositoryV5 implements MemberRepository{

    // * 기존의 의존성 변경

    // 1. dataSource -> repository에서 직접 사용하지 않고, template을 통해서 사용
    //private final DataSource dataSource;

    // 2. ExceptionTranslator -> 예외 변환 또한 tempalte에서 모두 처리해줌
    //private final SQLExceptionTranslator exTranslator;

    private final JdbcTemplate template;

    public MemberRepositoryV5(DataSource dataSource) {
        //this.dataSource = dataSource;
        //this.exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public Member save(Member member) {
        String sql = "insert into member(member_id, money) values(?, ?)";

        template.update(sql, member.getMemberId(), member.getMoney());
        // 반환값 사용시, update 된 숫자로 사용 가능

        return member;

    /*
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
            DataAccessException ex = exTranslator.translate("save", sql, e);
            throw ex;
        } finally {
            close(con, pstmt, null);
        }
    */

    }

    @Override
    public Member findById(String memberId) {
        String sql = "select * from member where member_id = ?";

        // * select의 경우, rs를 읽어와 Member 객체에 매핑했던 것처럼, 결과를 매핑할 수단이 필요함
        Member member = template.queryForObject(sql, memberRowMapper(), memberId);
        return member;

    /*
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
    */

    }

    private RowMapper<Member> memberRowMapper() {
        return (rs, rowNum) -> {
            Member member = new Member();
            member.setMemberId(rs.getString("member_id"));
            member.setMoney(rs.getInt("money"));
            return member;
        };
    }

    @Override
    public void update(String memberId, int money) {
        String sql = "update member set money=? where member_id=?";

        template.update(sql, money, memberId);

    /*
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
    */

    }

    @Override
    public void delete(String memberId) {
        String sql = "delete from member where member_id=?";

        template.update(sql, memberId);

    /*
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
    */

    }

/*  // *** 리소스 release / 커넥션 동기화 -> 이것도 template에서 모두 처리해줌
    private void close(Connection con, Statement stmt, ResultSet rs) {
        JdbcUtils.closeResultSet(rs);
        JdbcUtils.closeStatement(stmt);
        DataSourceUtils.releaseConnection(con, dataSource);
    }

    private Connection getConnection() throws SQLException {
        Connection con = DataSourceUtils.getConnection(dataSource);
        log.info("get connection = {}, class = {}", con, con.getClass());
        return con;
    }
*/

}
