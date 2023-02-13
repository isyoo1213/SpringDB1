package springDB.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class UncheckedAppTest {

    /**
     * 문제점 해결
     * 1. Service, Controller 계층에 특정 기술에 국한된 Exception에 의존 해결
     * 2. 목적성이 존재하지 않는 throwing 체인의 제거
     * + 문서화 or 명시적 throws를 통해 발생 가능한 Exception에 대한 정보 전달 또한 사용 가능
     */

    @Test
    void unchecked() {
        Controller controller = new Controller();
        assertThatThrownBy(() -> controller.request()).isInstanceOf(MyRuntimeSQLException.class);
    }

    @Test
    void printEx() {
        Controller controller = new Controller();
        try {
            controller.request();
        } catch (Exception e) {
            //printStackTrace()를 호출하는 것 보다 log를 남기는 것이 더 바람직
            // * but, System.out에 출력하기 위해서는 사용 가능
            //e.printStackTrace();
            log.info("ex", e);
        }
    }

    static class Controller {
        Service service = new Service();

        // Service에서 제거된 명시적 throwing Exception이 사라지므로, 여기에서도 명시적으로 throws 할 것이 없어짐
        public void request() {
            service.logic();
        }
    }

    static class Service {
        Repository repository = new Repository();
        NetworkClient networkClient = new NetworkClient();

        // *** 이제 Service 계층의 logic() 메서드에서는 repository에서 발생한 checked Exception를 명시적으로 throwing 할 필요 없음
        // -> Service를 호출하는 Controller에서도 checked Exception을 throwing할 필요 없음
        public void logic() {
            repository.call();
            networkClient.call();
        }

    }

    static class NetworkClient {
        public void call() {
            throw new MyRuntimeConnectException("연결 실패");
        }
    }

    static class Repository {
        // * 이제 call() 메서드 내에서 Exception 발생 시, 메서드 내부에서 처리해줄 예정
        // -> 단순히 외부로 throwing이 아닌, 기존 Exception을 RuntimeException으로 감싸서 throw(다시 발생시켜버리기)
        // -> *** 즉 'catch' 후 Exception을 바꿔서 'throws'하기
        public void call() {
            try {
                runSQL();
            } catch (SQLException e) {
                throw new MyRuntimeSQLException(e.getMessage(), e);
            }
        }

        public void runSQL() throws SQLException {
            throw new SQLException("ex");
        }
    }

    static class MyRuntimeConnectException extends RuntimeException {
        public MyRuntimeConnectException(String message) {
            super(message);
        }
    }

    static class MyRuntimeSQLException extends RuntimeException {

        // * Throwable인 cause를 parameter로 받으면, 이 예외를 발생시킨 예외 e 또한 받아서 활용 가능
        // - checked를 uncheked로 바꿔서 throw할 예정이므로, stackTrace를 위해 기존 checked를 확인하기 위한 생성자 파라미터
        public MyRuntimeSQLException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
