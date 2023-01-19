package springDB.jdbc.repository;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import springDB.jdbc.domain.Member;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MemberRepositoryV0Test {

    MemberRepositoryV0 repository = new MemberRepositoryV0();

    @Test
    void crud() throws SQLException {

        //save
        Member member = new Member("memberV2", 10000);
        repository.save(member); //save()에 Checked Exception을 throwing하고 있으므로 받아주어야함

        //findById
        Member findMember = repository.findById(member.getMemberId());

        log.info("findMember = {}", findMember);
        // @Data의 toString() 메서드가 오버라이딩 됐으므로, 참조값이 아닌 실제값 로깅

        log.info("findMember == member = {}", findMember == member);
        // *** 실제로는 다른 인스턴스이므로 당연히 false
        
        log.info("findMember.equals(member) = {}", findMember.equals(member));
        // -> but, @Data의 EqualsAndHashCode를 통해 오버라이딩됨 -> 값만 비교할 수 있음

        // -> assertThat()의 isEqualTo() 또한 내부적으로는 비슷한 원리로 값을 비교
        assertThat(findMember).isEqualTo(member);
    }
}