package nextstep.subway.common;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class StaticResourcesTest extends AcceptanceTest {
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * When javascript 정적 자원을 최초로 요청하면
     * Then 응답 상태 코드가 200 OK, 헤더에 ETag 가 존재, no-cache, private, 바디가 전송된다.
     * Given css 정적 자원을 초기화하고
     * When 최초로 요청하면
     * Then 응답 상태 코드가 200 OK, 헤더에 ETag 가 존재, max-age 는 1년, 바디가 전송된다.
     * When 헤더에 ETag 를 담아서 변경되지 않은 정적 자원을 요청하면
     * Then 응답 상태 코드가 304 Not Modified, 바디가 비어있다.
     * When 정적 자원을 변경하고 헤더에 ETag 를 담아서 요청하면
     * Then 응답 상태 코드가 200 OK, 헤더에 ETag 가 존재, 바디가 전송된다.
     */
    @DisplayName("정적 자원 테스트")
    @Test
    void staticResource() {
        // when
        Response javascriptResponse = RestAssured.given()
                .get("/js/main.js");

        // then
        assertThat(javascriptResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(javascriptResponse.getHeader(HttpHeaders.ETAG)).isNotBlank();
        assertThat(javascriptResponse.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache, private");
        assertThat(javascriptResponse.getBody().asString()).isNotBlank();

        // given
        String resourcePath = "/css/test.css";
        initCss(resourcePath);

        // when
        Response firstResponse = RestAssured.given()
                .get(resourcePath);
        String etagValue = firstResponse.getHeader(HttpHeaders.ETAG);

        // then
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(firstResponse.getHeader(HttpHeaders.ETAG)).isNotBlank();
        assertThat(firstResponse.getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("max-age=31536000");
        assertThat(firstResponse.getBody().asString()).isNotBlank();

        // when
        Response secondResponse = RestAssured.given()
                .headers("If-None-Match", etagValue)
                .get(resourcePath);

        // then
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
        assertThat(secondResponse.getBody().asString()).isEmpty();

        // when
        modifyCss(resourcePath); // 정적 리소스 변경
        Response thirdResponse = RestAssured.given()
                .headers("If-None-Match", etagValue)
                .get(resourcePath);

        // then
        assertThat(thirdResponse.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(thirdResponse.getHeader(HttpHeaders.ETAG)).isNotBlank();
        assertThat(thirdResponse.getBody().asString()).isNotBlank();
    }

    private void modifyCss(String path) {
        writeCss(path, "body {\n" +
                "    font-size: 50px;\n" +
                "}\n");
    }

    private void initCss(String path) {
        writeCss(path, "body {\n" +
                "    font-size: 14px;\n" +
                "}\n");
    }

    private void writeCss(String path, String content) {
        FileWriter fileWriter = null;
        try {
            Resource resource = applicationContext.getResource("classpath:/static" + path);
            File file = resource.getFile();
            fileWriter = new FileWriter(file);
            fileWriter.append(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
