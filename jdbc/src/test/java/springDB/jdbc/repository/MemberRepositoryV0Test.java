package springDB.jdbc.repository;

import org.junit.jupiter.api.Test;
import springDB.jdbc.domain.Member;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class MemberRepositoryV0Test {

    MemberRepositoryV0 repository = new MemberRepositoryV0();

    @Test
    void crud() throws SQLException {
        Member member = new Member("memberV0", 10000);
        repository.save(member); //save()에 Checked Exception을 throwing하고 있으므로 받아주어야함
    }
}