package nextstep.subway.member;

import nextstep.subway.AcceptanceTest;
import nextstep.subway.member.dto.MemberResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회원 관련 기능")
public
class MemberAcceptanceTest extends AcceptanceTest {
    public static final String EMAIL = "email@email.com";
    public static final String PASSWORD = "password";
    public static final int AGE = 20;

    @DisplayName("회원 정보를 관리한다.")
    @Test
    void manageMember() {
        // when
        ResponseEntity<Void> createResponse = 회원_등록되어_있음(EMAIL, PASSWORD, AGE);
        // then
        회원_생성됨(createResponse);

        // when
        Mono<ResponseEntity<MemberResponse>> findResponse = 회원_정보_조회_요청(createResponse);
        // then
        회원_정보_조회됨(findResponse, EMAIL, AGE);

        // when
        Mono<ResponseEntity<Void>> updateResponse = 회원_정보_수정_요청(createResponse, "new" + EMAIL, "new" + PASSWORD,
                                                                   AGE + 2);
        // then
        회원_정보_수정됨(updateResponse);

        // when
        Mono<ResponseEntity<Void>> deleteResponse = 회원_삭제_요청(createResponse);
        // then
        회원_삭제됨(deleteResponse);
    }

    public Mono<ResponseEntity<MemberResponse>> 회원_정보_조회_요청(ResponseEntity<Void> response) {
        String uri = response.getHeaders().getFirst("Location");
        assert uri != null;

        return webClient().get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(MemberResponse.class));
    }

    public Mono<ResponseEntity<Void>> 회원_정보_수정_요청(ResponseEntity<Void> response, String email,
                                                            String password, Integer age) {
        String uri = response.getHeaders().getFirst("Location");
        assert uri != null;

        return webClient().put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of("email", email, "password", password, "age", age)))
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }

    public Mono<ResponseEntity<Void>> 회원_삭제_요청(ResponseEntity<Void> response) {
        String uri = response.getHeaders().getFirst("Location");
        assert uri != null;

        return webClient().delete()
                .uri(uri)
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }

    public static void 회원_생성됨(ResponseEntity<Void> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
    }

    public static void 회원_정보_조회됨(Mono<ResponseEntity<MemberResponse>> response, String email, int age) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                    MemberResponse memberResponse = r.getBody();
                    assertThat(memberResponse).isNotNull();
                    assertThat(memberResponse.getId()).isNotNull();
                    assertThat(memberResponse.getEmail()).isEqualTo(email);
                    assertThat(memberResponse.getAge()).isEqualTo(age);
                })
                .verifyComplete();
    }

    public static void 회원_정보_수정됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                })
                .verifyComplete();
    }

    public static void 회원_삭제됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                })
                .verifyComplete();
    }
}
