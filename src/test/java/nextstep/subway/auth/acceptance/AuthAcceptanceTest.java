package nextstep.subway.auth.acceptance;

import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.member.dto.MemberResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static nextstep.subway.member.MemberAcceptanceTest.회원_정보_조회됨;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bearer Auth 관련 기능")
class AuthAcceptanceTest extends AcceptanceTest {
    private static final String EMAIL = "email@email.com";
    private static final String PASSWORD = "password";
    private static final Integer AGE = 20;

    @DisplayName("Bearer Auth")
    @Test
    void myInfoWithBearerAuth() {
        회원_등록되어_있음(EMAIL, PASSWORD, AGE);
        TokenResponse tokenResponse = 로그인_되어_있음(EMAIL, PASSWORD);

        Mono<ResponseEntity<MemberResponse>> response = 내_회원_정보_조회_요청(tokenResponse);

        회원_정보_조회됨(response, EMAIL, AGE);
    }

    @DisplayName("Bearer Auth 로그인 실패")
    @Test
    void myInfoWithBadBearerAuth() {
        회원_등록되어_있음(EMAIL, PASSWORD, AGE);

        Mono<ResponseEntity<TokenResponse>> response = 로그인_요청(EMAIL + "OTHER", PASSWORD);

        로그인_실패(response);
    }

    @DisplayName("Bearer Auth 유효하지 않은 토큰")
    @Test
    void myInfoWithWrongBearerAuth() {
        TokenResponse tokenResponse = new TokenResponse("accesstoken");

        Mono<ResponseEntity<MemberResponse>> response = 내_회원_정보_조회_요청(tokenResponse);

        내_회원_정보_조회_실패(response);
    }

    private static void 로그인_실패(Mono<ResponseEntity<TokenResponse>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verifyComplete();
    }

    public Mono<ResponseEntity<MemberResponse>> 내_회원_정보_조회_요청(TokenResponse tokenResponse) {
        return webClient().get()
                .uri("/members/me")
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", tokenResponse.getAccessToken()))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(MemberResponse.class));
    }

    private static void 내_회원_정보_조회_실패(Mono<ResponseEntity<MemberResponse>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verifyComplete();
    }
}
