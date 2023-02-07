package springDB.jdbc.service;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import springDB.jdbc.domain.Member;
import springDB.jdbc.repository.MemberRepositoryV3;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static springDB.jdbc.connection.ConnectionConst.*;

/**
 * Transaction - @Transactional AOP 사용
 */
@Slf4j
@SpringBootTest //Spring Container에 Bean 등록 및 의존 관계 주입을 위해
class MemberServiceV3_3Test {

    // *** 현재 Test에서는 Spring Container를 전혀 사용하지 않고 있음
    // - Spring Container에 Bean을 등록하거나 관리하지 않고, 원하는 요소들을 직접 제어하고 있음
    // -> *** Spring이 제공하는 AOP를 활용하기 위해선 Spring Container에 관련 Bean들이 모두 등록되어야 함
    // -> *** @SpringBootTest를 통해, Test실행 시 Container를 띄우고 Bean들을 등록하며 의존관계를 주입받으며 연동된 Junit 기능들을 활용

    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

    @Autowired
    private MemberRepositoryV3 memberRepository;

    @Autowired
    private MemberServiceV3_3 memberService;
    // *** 실제로 주입받는 인스턴스는 transaction 처리 로직을 위한 Proxy 클래스의 인스턴스

    @TestConfiguration
    static class TestConfig {

        //dataSource는 transactionManager, Repository에서 사용
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        }

        //transactionManager 또한 AOP를 위한 Proxy에서 주입받아서 transaction 관련된 처리
        // *** 원칙적으로 Spring AOP를 통한 transaction은 Bean을 통한 transactionManager를 사용하므로, 꼭 등록해주어야 함
        @Bean
        PlatformTransactionManager transactionManager() {
            return new DataSourceTransactionManager(dataSource());
        }

        @Bean
        MemberRepositoryV3 memberRepositoryV3() {
            return new MemberRepositoryV3(dataSource());
        }

        @Bean
        MemberServiceV3_3 memberServiceV3_3() {
            return new MemberServiceV3_3(memberRepositoryV3());
        }
    }

/*
    // * Spring Container에 등록된 Bean 사용을 위해 dataSource, repository, transactionManager에 대한 Before() 설정 X
    @BeforeEach
    void before() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        memberRepository = new MemberRepositoryV3(dataSource);

        // *** 이제 V3_3에서 Service는 transaction과 관련된 직접적인 의존 X
        //PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        memberService = new MemberServiceV3_3(memberRepository);
    }
*/

    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    void aopCheck() {
        log.info("memberService class = {}", memberService.getClass());
        //Service의 실제 로그
        //memberService class = class springDB.jdbc.service.MemberServiceV3_3$$SpringCGLIB$$0

        // *** 즉, 'transaction 처리 로직'을 위한 Proxy 클래스
        // cf) 이후 '비즈니스 로직'을 처리할 실제 Service 클래스의 '로직'
        // -> *** 즉, TestConfig에서 주입받은 Service는 transaction 처리 로직을 위한 Proxy 클래스이며
        //    + 이 proxy 클래스가 실제 Service 클래스의 @Transactional 어노테이션이 붙은 메서드의 실제 bizLogic을 호출해내는 방식

        log.info("memberRepository class = {}", memberRepository.getClass());
        // Repository의 실제 로그
        //emberRepository class = class springDB.jdbc.repository.MemberRepositoryV3

        //Aop의 Proxy 인스턴스인지 확인하는 기능
        assertThat(AopUtils.isAopProxy(memberService)).isTrue();
        assertThat(AopUtils.isAopProxy(memberRepository)).isFalse();
    }

    @Test
    @DisplayName("정상이체")
    void accountTransfer() throws SQLException {
        //given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);

        //when
        // *** Service에서 호출한 메서드가 동일한 conn을 사용하는지(추가적으로 conn을 생성하지 않는지) 확인 필수
        log.info("START TX");
        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);
        log.info("END TX");

        //then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransferEx() throws SQLException {
        //given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);

        //when
        assertThatThrownBy(() -> memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);

        //then - accountTransfer() 메서드에서, 예외 발생 후 toId계좌의 금액 처리 로직에 접근하지 못하므로 변동이 없음을 검증
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberEx = memberRepository.findById(memberEx.getMemberId());

        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberEx.getMoney()).isEqualTo(10000);
    }

}