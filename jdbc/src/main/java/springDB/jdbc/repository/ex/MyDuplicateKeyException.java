package springDB.jdbc.repository.ex;

// * MyDbException을 상속받기
// -> DB에서 발생한 Exception이라는 계층을 구성할 수 있음
// + 직접 구성한 Exception을 상속받으므로, JDBC/JPA 등의 구체적인 기술에 의존X
public class MyDuplicateKeyException extends MyDbException{

    public MyDuplicateKeyException() {
    }

    public MyDuplicateKeyException(String message) {
        super(message);
    }

    public MyDuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyDuplicateKeyException(Throwable cause) {
        super(cause);
    }

}
