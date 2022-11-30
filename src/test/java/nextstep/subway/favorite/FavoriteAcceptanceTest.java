package nextstep.subway.favorite;

import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.favorite.dto.FavoriteResponse;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("즐겨찾기 관련 기능")
public class FavoriteAcceptanceTest extends AcceptanceTest {
    public static final String EMAIL = "email@email.com";
    public static final String PASSWORD = "password";

    private LineResponse 신분당선;
    private StationResponse 강남역;
    private StationResponse 양재역;
    private StationResponse 정자역;
    private StationResponse 광교역;

    private TokenResponse 사용자;

    @BeforeEach
    public void setUp() {
        super.setUp();

        강남역 = 지하철역_등록되어_있음("강남역").getBody();
        양재역 = 지하철역_등록되어_있음("양재역").getBody();
        정자역 = 지하철역_등록되어_있음("정자역").getBody();
        광교역 = 지하철역_등록되어_있음("광교역").getBody();

        Map<String, String> lineCreateParams;
        lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", "신분당선");
        lineCreateParams.put("color", "bg-red-600");
        lineCreateParams.put("upStationId", 강남역.getId() + "");
        lineCreateParams.put("downStationId", 광교역.getId() + "");
        lineCreateParams.put("distance", 10 + "");
        신분당선 = 지하철_노선_등록되어_있음(lineCreateParams).getBody();

        지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, 3);
        지하철_노선에_지하철역_등록_요청(신분당선, 양재역, 정자역, 3);

        회원_등록되어_있음(EMAIL, PASSWORD, 20);
        사용자 = 로그인_되어_있음(EMAIL, PASSWORD);
    }

    @DisplayName("즐겨찾기를 관리한다.")
    @Test
    void manageMember() {
        // when
        ResponseEntity<Void> createResponse = 즐겨찾기_생성을_요청(사용자, 강남역, 정자역);
        // then
        즐겨찾기_생성됨(createResponse);

        // when
        Mono<ResponseEntity<List<FavoriteResponse>>> findResponse = 즐겨찾기_목록_조회_요청(사용자);
        // then
        즐겨찾기_목록_조회됨(findResponse);

        // when
        Mono<ResponseEntity<Void>> deleteResponse = 즐겨찾기_삭제_요청(사용자, createResponse);
        // then
        즐겨찾기_삭제됨(deleteResponse);
    }

    public ResponseEntity<Void> 즐겨찾기_생성을_요청(TokenResponse tokenResponse, StationResponse source,
                                            StationResponse target) {
        return webClient().post()
                .uri("/favorites")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", tokenResponse.getAccessToken()))
                .body(BodyInserters.fromValue(
                        Map.of("source", source.getId().toString(), "target", target.getId().toString())))
                .exchangeToMono(ClientResponse::toBodilessEntity)
                .block();
    }

    public Mono<ResponseEntity<List<FavoriteResponse>>> 즐겨찾기_목록_조회_요청(TokenResponse tokenResponse) {
        return webClient().get()
                .uri("/favorites")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", tokenResponse.getAccessToken()))
                .exchangeToMono(clientResponse -> clientResponse.toEntityList(FavoriteResponse.class));
    }

    public Mono<ResponseEntity<Void>> 즐겨찾기_삭제_요청(TokenResponse tokenResponse,
                                                           ResponseEntity<Void> response) {
        String uri = response.getHeaders().getFirst("Location");
        assert uri != null;

        return webClient().delete()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", tokenResponse.getAccessToken()))
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }

    public static void 즐겨찾기_생성됨(ResponseEntity<Void> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    public static void 즐겨찾기_목록_조회됨(Mono<ResponseEntity<List<FavoriteResponse>>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(r.getBody()).isNotEmpty();
                })
                .verifyComplete();
    }

    public static void 즐겨찾기_삭제됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                })
                .verifyComplete();
    }
}
