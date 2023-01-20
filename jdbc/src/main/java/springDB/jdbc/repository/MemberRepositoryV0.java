package springDB.jdbc.repository;

import lombok.extern.slf4j.Slf4j;
import springDB.jdbc.connection.DBConnectionUtil;
import springDB.jdbc.domain.Member;

import java.sql.*;
import java.util.NoSuchElementException;

/**
 * JDBC - DriverManager 사용
 */

@Slf4j
public class MemberRepositoryV0 {

    public Member save(Member member) throws SQLException {
        String sql = "insert into member(member_id, money) values(?, ?)";

        Connection con = null;
        PreparedStatement pstmt = null;
        //Statement를 상속 - parameter Binding할 수 있음
        // + *** SQL Injection 공격을 피하기 위해서 pstmt을 통해 바인딩하는 방식을 추천
        // * try-catch 때문에 밖에서 null로 초기화해주어야함

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());

            // *** pstmt에 parameter 바인딩 시, '영향 받은 DB row 수'만큼의 int 값을 반환값으로 사용 가능
            // -> 여기에서는 1개의 row를 추가했으므로 1을 반환
            pstmt.executeUpdate();

            return member;
        } catch (SQLException e) { //오류 발생 시 로그 정도만 확인하고 예외를 던지도록 구성
            log.error("db error = {}", e);
            throw e;
        } finally { // *** 리소스 정리 - 실제 tcp/ip 커넥션을 통해 외부 리소스를 사용하는 것이므로 + 역순으로 진행
            close(con, pstmt, null);
            //현제 rs는 없으므로 null로 전달 - * ResultSet - 쿼리로 결과를 '조회'할 때 사용 (즉, DB로부터 반환값이 있을 때)
        }
    }

    public Member findById(String memberId) throws SQLException {
        String sql = "select * from member where member_id = ?";

        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);

            pstmt.setString(1, memberId);

            //SELECT 조회 시, ResultSEt 반환
            rs = pstmt.executeQuery();
            if (rs.next()) { //rs에서 최초로 next() 호출 해주어야 *Cursor가 실제 data 접근
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            } else {
                throw new NoSuchElementException("member not found. memberId = " + memberId);
            }
        } catch (SQLException e) {
            log.error("db error = {}", e);
            throw e;
        } finally {
            close(con, pstmt, rs);
        }
    }

    public void update(String memberId, int money) throws SQLException {
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
            throw e;
        } finally {
            close(con, pstmt, null);
        }

    }

    public void delete(String memberId) throws SQLException {
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
            throw e;
        } finally {
            close(con, pstmt, null);
        }

    }

    private void close(Connection con, Statement stmt, ResultSet rs) {
    // *** 순차적인 종료가 필요한 상황에서 stmt close시 Exception발생 하더라도, con의 close에는 영향 주지 않도록 구성

        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.info("error = {}", e);
            }
        }

        if(stmt != null) {
            try {
                stmt.close(); //Checked Exception 발생 가능 ->
            } catch (SQLException e) { //실제 스테이트먼트 종료 시 exception발생 시 할 수 있는 것이 없으므로 로그 정도만
                log.info("error = {}", e);
            }
        }

        if(con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                log.info("error = {}", e);
            }
        }
    }

    private Connection getConnection() {
        return DBConnectionUtil.getConnection();
    }

}
