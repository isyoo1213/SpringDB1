package springDB.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Slf4j
class CheckedTest {

    @Test
    void checked_catch() {
        Service service = new Service();
        service.callCatch();
        // callCatch() 호출 후, catch를 통해 예외 처리를 수행 후 checked_catch() 테스트로 빠져나오고, 로직이 종료됨
        // -> 정상흐름으로 checked_catch() 테스트 메서드가 종료되므로 테스트 성공이 뜨게 됨
    }

    @Test
    void checked_throw() {
        Service service = new Service();
        assertThatThrownBy(() -> service.callThrow()).isInstanceOf(MyCheckedException.class);
    }

    // * catch와 throws 모두 부모 Exception으로 처리 가능

    /**
     * Exception을 상속받은 예외는 Checked 예외가 된다
     */
    static class MyCheckedException extends Exception {
        public MyCheckedException(String message) {
            super(message);
        }
    }

    /**
     * Checked Exception은
     * try-catch 하거나 throwing하거나 둘 중 하나로 필수 처리해주어야 함
     */
    static class Service {
        Repository repository = new Repository();

        /**
         * 예외를 잡아서 처리하는 코드
         */
        public void callCatch() {
            try {
                repository.call();
            } catch (MyCheckedException e) {
                //예외 처리 로직
                log.info("예외 처리, message = {}", e.getMessage(), e);
                //exception을 stackTrace 할 때에는 그냥 객체만 마지막 parameter에 넣어주면 됨
            }
        }

        /**
         * checked 예외를 호출한 메서드로 던지는 코드 by throws
         * 예외처리를 하지 않는 경우 compile에 필수
         */
        public void callThrow() throws MyCheckedException {
            repository.call();
        }

    }

    static class Repository {
        public void call() throws MyCheckedException {
            throw new MyCheckedException("ex");
        }
    }
}
