package springDB.jdbc.exception.basic;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@Slf4j
class UncheckedTest {

    @Test
    void unchecked_catch() {
        Service service = new Service();
        service.callCatch();
    }

    @Test
    void unchecked_throw() {
        Service service = new Service();
        assertThatThrownBy(()->service.callThrow()).isInstanceOf(MyUncheckedException.class);
    }

    /**
     * RuntimeException을 상속받은 예외는 Unchecked Exception
     */
    static class MyUncheckedException extends RuntimeException {
        public MyUncheckedException(String message) {
            super(message);
        }
    }

    /**
     * Unchecked Exception은 catch or throws 하지 않아도 된다
     * catch하지 않는 경우, 자동으로 throws 처리
     */
    static class Service {
        Repository repository = new Repository();

        /**
         * 필요한 경우 exception을 catch해서 처리해줘도 된다
         * 즉, checked와 unchecked의 차이는 "compiler"가 체크를 하는지의 여부가 가장 중요
         */
        public void callCatch() {
            try {
                repository.call();
            } catch (MyUncheckedException e) {
                log.info("예외 처리, message = {}", e.getMessage(), e);
            }
        }

        /**
         * 예외를 명시적으로 throws 하지 않아도 된다. 자연스럽게 상위로 넘어간다.
         */
        public void callThrow() {
            repository.call();
        }
    }

    static class Repository {
        public void call() { // * unchecked는 throws 생략 가능 (선언 해도 된다)
            throw new MyUncheckedException("ex");
        }
    }

}
