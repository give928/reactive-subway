package nextstep.subway.common;

import nextstep.subway.AcceptanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("정적 자원 관련 기능")
class StaticResourcesTest extends AcceptanceTest {
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * When javascript 정적 자원을 최초로 요청하면
     * Then 응답 상태 코드가 200 OK, 헤더에 ETag 가 존재, 압축 설정, no-cache, private, 바디가 전송된다.
     * Given css 정적 자원을 초기화하고
     * When 최초로 요청하면
     * Then 응답 상태 코드가 200 OK, 헤더에 ETag 가 존재, 압축 설정, max-age 는 1년, 바디가 전송된다.
     * When 헤더에 ETag 를 담아서 변경되지 않은 정적 자원을 요청하면
     * Then 응답 상태 코드가 304 Not Modified, 바디가 비어있다.
     * When 정적 자원을 변경하고 헤더에 ETag 를 담아서 요청하면
     * Then 응답 상태 코드가 200 OK, 헤더에 ETag 가 존재, 바디가 전송된다.
     */
    @DisplayName("정적 자원 테스트")
    @Test
    void staticResource() {
        javascript();

        css();
    }

    private void javascript() {
        // when
        Mono<ResponseEntity<String>> javascriptResponse = webClient().get()
                .uri("/js/main.js")
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class));

        // then
        StepVerifier.create(javascriptResponse)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
//                    assertThat(r.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING)).isEqualTo("gzip");
                    assertThat(r.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING)).isEqualTo("chunked");
                    assertThat(r.getHeaders().getFirst(HttpHeaders.ETAG)).isNotBlank();
                    assertThat(r.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache, private");
                    assertThat(r.getBody()).isNotBlank();
                })
                .verifyComplete();
    }

    private void css() {
        // given
        String resourcePath = "/css/test.css";
        initCss(resourcePath);

        // when
        ResponseEntity<String> firstResponse = webClient().get()
                .uri(resourcePath)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class))
                .block();

        // then
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getHeaders().getFirst(HttpHeaders.ETAG)).isNotBlank();
        assertThat(firstResponse.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("max-age=31536000");
        assertThat(firstResponse.getBody()).isNotBlank();

        // given
        String etagValue = firstResponse.getHeaders().getFirst(HttpHeaders.ETAG);

        // when
        Mono<ResponseEntity<String>> secondResponse = webClient().get()
                .uri(resourcePath)
                .header(HttpHeaders.IF_NONE_MATCH, etagValue)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class));

        // then
        StepVerifier.create(secondResponse)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
                    assertThat(r.getBody()).isNullOrEmpty();
                })
                .verifyComplete();

        // when
        modifyCss(resourcePath); // 정적 리소스 변경
        Mono<ResponseEntity<String>> thirdResponse = webClient().get()
                .uri(resourcePath)
                .header(HttpHeaders.IF_NONE_MATCH, etagValue)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(String.class));

        // then
        StepVerifier.create(thirdResponse)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(r.getHeaders().getFirst(HttpHeaders.ETAG)).isNotBlank();
                    assertThat(r.getBody()).isNotBlank();
                })
                .verifyComplete();

        deleteCss(resourcePath);
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
        File file;
        try {
            Resource resource = applicationContext.getResource("classpath:/static" + path);
            if (!resource.exists()) {
                file = new File(resource.createRelative("")
                                        .getFile()
                                        .getAbsolutePath() + "/" + resource.getFilename());
                file.createNewFile();
            } else {
                file = resource.getFile();
            }
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append(content);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteCss(String path) {
        try {
            Resource resource = applicationContext.getResource("classpath:/static" + path);
            File file = resource.getFile();
            file.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
