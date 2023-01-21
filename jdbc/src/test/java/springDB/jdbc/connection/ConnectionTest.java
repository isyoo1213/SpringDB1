package springDB.jdbc.connection;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static springDB.jdbc.connection.ConnectionConst.*;

@Slf4j
class ConnectionTest {

    @Test
    void dirveManager() throws SQLException {
        //DriverManger를 통해 Connection을 2번 획득했을 때의 정보 확인
        Connection con1 = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        Connection con2 = DriverManager.getConnection(URL, USERNAME, PASSWORD);

        log.info("connection = {}, class = {}", con1, con1.getClass());
        log.info("connection = {}, class = {}", con2, con2.getClass());
    }

    //스프링이 제공하는 DriveMangerDataSource 사용
    @Test
    void dataSourceDriverManager() throws SQLException {
        // *** DriveMangerDataSource는 항상 새로운 connection을 획득
        // + 부모 클래스를 따라가다보면 DataSource를 implements하고 있으므로 DataSource 자료형으로도 사용 가능

        DriverManagerDataSource dataSource =  new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        // *** '설정'과 '사용'의 분리
        // + '생성'하는 시점에만 URL, USERNAME, PASSWORD의 정보 입력 - 향후 변경에 더 유연하게 대처
        // + '사용'(Con 획득) 시점에는 그냥 사용 - 향후 변경에도 getConnection()만 호출해서 사용 가능
        // -> *** 즉, Repository는 DataSource 인스턴스에만 의존하고, 나머지 정보에는 의존하지 않는다

        useDataSource(dataSource);
        // -> 결과는 똑같지만, 추상화된 DatasSource 인터페이스를 사용하는 방식
    }

    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();

        log.info("connection = {}, class = {}", connection1, connection1.getClass());
        log.info("connection = {}, class = {}", connection2, connection2.getClass());
    }
}
