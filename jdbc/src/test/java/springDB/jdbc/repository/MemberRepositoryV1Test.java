package springDB.jdbc.repository;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import springDB.jdbc.domain.Member;

import java.sql.SQLException;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static springDB.jdbc.connection.ConnectionConst.*;

@Slf4j
class MemberRepositoryV1Test {

    MemberRepositoryV1 repository;

    @BeforeEach
    void beforeEach() {
        //기본 DriverManager를 통한 항상 새로운 connection을 획득하는 방식
        // *** sql을 날릴 때마다 새로운 con0~5를 획득
        //DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);

        //커넥션 풀링 - con0~9까지의 Pool을 채운 후 Wrapping conn0을 그대로 재사용
        // *** 계속 생성되고 제거되는 HikariProxyConnection의 객체에 담아서 들어오므로 인스턴스 주소는 다를 수 있으나, 1개의 커넥션을 사용, 반환, 대기하는 사이클은 동일
        HikariDataSource dataSource = new HikariDataSource();
        //DataSource 자료형으로 받을 수도 있지만, 인터페이스므로 아래의 set메서드들 사용 불가능하므로 구체화된 클래스 사용
        dataSource.setJdbcUrl(URL);
        dataSource.setUsername(USERNAME);
        dataSource.setPassword(PASSWORD);

        repository = new MemberRepositoryV1(dataSource);
    }

    @Test
    void crud() throws SQLException {

        //save
        Member member = new Member("memberV0", 10000);
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

        //update - money : 10000 -> 20000
        repository.update(member.getMemberId(), 20000);
        Member updatedMember = repository.findById(member.getMemberId());

        assertThat(updatedMember.getMoney()).isEqualTo(20000);

        //delete
        repository.delete(member.getMemberId());

        assertThatThrownBy(() -> repository.findById(member.getMemberId()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // *** 현재 Test의 문제점
    // 중간에 오류가 발생할 시, 윗 단에서 이루어진 DB에서의 수정이 그대로 반영되어버림
    // -> 이후 transaction에서 다룰 예정

    // + 현재 LOCAL 상태에서 Test실행 시 Connection Pool이 획득한 conn의 갯수가 6~7개까지만 로깅됨
    // -> thread의 sleep() 조정을 해보거나 원인 파악 필요
}