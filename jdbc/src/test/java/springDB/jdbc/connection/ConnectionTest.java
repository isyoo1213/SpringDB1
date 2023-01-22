package springDB.jdbc.connection;

import com.zaxxer.hikari.HikariDataSource;
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

    @Test
    void dataSourceConnectionPool() throws SQLException, InterruptedException {
        //커넥션 풀링

        //HikariDataSource - 이 또한 DataSource를 구현
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);
        dataSource.setMaximumPoolSize(10);
        dataSource.setPoolName("MyPool");

        // *** 커넥션을 획득하는 작업은 어플리케이션 실행에 영향을 주지 않기 위해 MyPool connection adder라는 별도의 thread에서 실행
        // Test실행 시, 별도의 thread에서 실행해 커넥션 풀을 획득하는 과정을 로깅하기 위해 1초정도 슬립을 줌
        // 슬립을 주지 않으면, test실행 thread의 처리 속도가 빨라 별도의 thread에서 획득한 커넥션 정보가 로그에 전달되지 않음
        useDataSource(dataSource);
        Thread.sleep(200);

        //로그 정보
        //[MyPool housekeeper] ... MyPool - Pool stats (total=7, active=2, idle=5, waiting=0)
        //3개는 test스레드 완료 후에 실행되어 total 7, 2개는 현재 사용중인 con은 2개, pool에 획득되어 대기상태인 idle은 5
        // *** Connection Pool에 connection을 채우는 작업은 상대적으로 오래 걸림
        // -> application 실행 시 대기를 줄이기 위해 별도의 thread 사용

        // + Pool에 1개의 커넥션만 획득한 상태에서 getConnection()호출 시, 나머지 추가적인 con이 pool에 채워지길 기다리는 내부로직 시행

        // + pool의 con 개수가 10개인 상황에서, 10개를 초과하는 getConnection()호출 시
        // -> 1개의 waiting 발생 -> 30초 후에 연결이 끊겨버림 - 설정을 통해 컨트롤 가능
    }

    private void useDataSource(DataSource dataSource) throws SQLException {
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();

        log.info("connection = {}, class = {}", connection1, connection1.getClass());
        log.info("connection = {}, class = {}", connection2, connection2.getClass());
    }
}
