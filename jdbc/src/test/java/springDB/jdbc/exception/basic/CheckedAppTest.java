package springDB.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

@Slf4j
class CheckedAppTest {

    /**
     * 현재의 문제점
     * 1. Service, Controller 계층에 특정 기술에 국한된 Exception에 의존하게 됨
     * 2. 그렇다고 throwing 체인이 특정 계층에서의 해결을 위한 목적성이 존재하지 않음
     * *** 최상위인 Exception으로 Throwing 하게되면, Checked 또한 모두 무시되므로 compile에서도 오류라고 인식하지 않음
     */

    @Test
    void checked() {
        Controller controller = new Controller();
        assertThatThrownBy(() -> controller.request()).isInstanceOf(Exception.class);
    }

    static class Controller {
        Service service = new Service();

        public void request() throws SQLException, ConnectException {
            service.logic();
        }
    }

    static class Service {
        Repository repository = new Repository();
        NetworkClient networkClient = new NetworkClient();

        public void logic() throws SQLException, ConnectException {
            repository.call();
            networkClient.call();
        }

    }

    static class NetworkClient {
        public void call() throws ConnectException {
            throw new ConnectException("연결 실패");
        }
    }

    static class Repository {
        public void call() throws SQLException {
            throw new SQLException("ex");
        }
    }

}
