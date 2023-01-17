package springDB.jdbc.connection;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static springDB.jdbc.connection.ConnectionConst.*;

@Slf4j
public class DBConnectionUtil {

    //java.squl의 Connection 사용 - *** JDBC 표준 인터페이스가 제공하는 Connection
    public static Connection getConnection() {
        try {

            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            // *** DriverManager가 h2 라이브러리 내의 Driver 클래스 정보를 찾아서 h2 드라이버를 호출 및 처리해줌
            // -> *** 즉, JDBC 표준 인터페이스가 '구현체'인 각각의 DB 드라이버를 찾아서 연결해줌
            // *** Connection은 Interface -> 구현체는 DB 드라이버 클래스인 jdbcConnection의 인스턴스
            // + 실제로 해당 클래스는 Connection을 implements하고있음
            // -> 구현체 로그 - class = class org.h2.jdbc.JdbcConnection
            // + Checked Exception -> try-catch로 잡아주어야함


            log.info("get connection = {}, class = {}", connection, connection.getClass());

            return connection;
        } catch (SQLException e) { // *** Checked Exception -> Runtime Exception으로 전환해서 던짐
            throw new IllegalStateException(e);
        }
    }
}
