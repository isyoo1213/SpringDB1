package springDB.jdbc.exception.translator;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import springDB.jdbc.connection.ConnectionConst;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;
import static springDB.jdbc.connection.ConnectionConst.*;

/**
 * Spring이 제공하는 Exception Translator 사용하기
 * SQLErrorCodeSQLExceptionTranslator
 * DataAccessException
 */

@Slf4j
class SpringExceptionTranslatorTest {

    DataSource dataSource;

    @BeforeEach
    void init() {
        dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    }

    @Test
    void sqlExceptionErrorCode() {
        String sql = "select bad grammar";

        try {
            Connection con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.executeQuery();
        } catch (SQLException e) {
            //H2 DB의 bad SQL 오류코드
            assertThat(e.getErrorCode()).isEqualTo(42122);

            // * 실제로 Service 계층으로 Exception을 넘기기 위해선, Repository에서 그에 맞는 Exception으로 변환하는 과정이 필요
            // *** but, DB와 ErrorCode에 따라 식별하고 처리해주는 로직은 기하급수적으로 증가 가능
            // ex) if (errorCode == ... ) { Throw new ...Exception(e) }

            int errorCode = e.getErrorCode();
            log.info("errorCode = {}", errorCode);
            log.info("error", e);
        }
    }

    @Test
    void exceptionTranslator() {

        String sql = "select bad grammar";

        try {
            Connection con = dataSource.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql);
            pstmt.executeQuery();
        } catch (SQLException e) {
            assertThat(e.getErrorCode()).isEqualTo(42122);

            // * Spring이 제공하는 Exception Translator - SQLErrorCodeSQLExceptionTranslator
            SQLErrorCodeSQLExceptionTranslator exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);

            // * P1 - 작업 이름 / P2 - 실행할 sql / P3 - 분석 적용할 Exception
            // -> 이에 따라 SQLException을 분석해서 DataAcceException 중의 하나의 Exception을 반환함
            // + sql-error-codes.xml 파일에 DB에 따른 errorCode의 값들이 설정되어 있음 - RDB
            // -> DB에서 반환해주는 error 정보에서 errorCode를 얻어와, xml정보와 매핑하면서 exception화
            DataAccessException resultEx = exTranslator.translate("select", sql, e);

            log.info("resultEx", resultEx);

            // * 이 경우, 잘못된 sql에 따른 오류이므로 BadSqlGrammarException이 반환되는지 체크
            assertThat(resultEx.getClass()).isEqualTo(BadSqlGrammarException.class);
        }
    }
}
