package springDB.jdbc.repository;

import springDB.jdbc.domain.Member;

import java.sql.SQLException;

public interface MemberRepositoryEx {

    // *** 구현체의 메서드에서 throws를 하기 위해선 인터페이스에서도 throws를 선언해주어야함
    // -> checked를 throws 하기 위해서는 결국 인터페이스 또한 특정 기술에 종속될 수밖에 없음

    Member save(Member member) throws SQLException;

    Member findById(String memberId) throws SQLException;

    void update(String memberId, int money) throws SQLException;

    void delete(String memberId) throws SQLException;
}
