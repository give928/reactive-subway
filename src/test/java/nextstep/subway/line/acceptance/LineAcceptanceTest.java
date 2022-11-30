package nextstep.subway.line.acceptance;

import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 노선 관련 기능")
public class LineAcceptanceTest extends AcceptanceTest {
    private StationResponse 강남역;
    private StationResponse 광교역;
    private Map<String, String> lineCreateParams;

    @BeforeEach
    public void setUp() {
        super.setUp();

        // given
        강남역 = 지하철역_등록되어_있음("강남역").getBody();
        광교역 = 지하철역_등록되어_있음("광교역").getBody();

        lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", "신분당선");
        lineCreateParams.put("color", "bg-red-600");
        lineCreateParams.put("upStationId", 강남역.getId() + "");
        lineCreateParams.put("downStationId", 광교역.getId() + "");
        lineCreateParams.put("distance", 10 + "");
    }

    @DisplayName("지하철 노선을 생성한다.")
    @Test
    void createLine() {
        // when
        Mono<ResponseEntity<LineResponse>> response = 지하철_노선_생성_요청(lineCreateParams);

        // then
        지하철_노선_생성됨(response);
    }

    @DisplayName("기존에 존재하는 지하철 노선 이름으로 지하철 노선을 생성한다.")
    @Test
    void createLineWithDuplicateName() {
        // given
        지하철_노선_등록되어_있음(lineCreateParams);

        // when
        Mono<ResponseEntity<LineResponse>> response = 지하철_노선_생성_요청(lineCreateParams);

        // then
        지하철_노선_생성_실패됨(response);
    }

    @DisplayName("지하철 노선 목록을 조회한다.")
    @Test
    void getLines() {
        // given
        Map<String, String> params = new HashMap<>();
        params.put("name", "구분당선");
        params.put("color", "bg-red-600");
        params.put("upStationId", 강남역.getId() + "");
        params.put("downStationId", 광교역.getId() + "");
        params.put("distance", 15 + "");
        ResponseEntity<LineResponse> createResponse1 = 지하철_노선_등록되어_있음(params);
        ResponseEntity<LineResponse> createResponse2 = 지하철_노선_등록되어_있음(lineCreateParams);

        // when
        Mono<ResponseEntity<List<LineResponse>>> response = 지하철_노선_목록_조회_요청();

        // then
        지하철_노선_목록_응답됨(response);
        지하철_노선_목록_포함됨(response, Arrays.asList(createResponse1, createResponse2));
    }

    @DisplayName("지하철 노선을 조회한다.")
    @Test
    void getLine() {
        // given
        ResponseEntity<LineResponse> createResponse = 지하철_노선_등록되어_있음(lineCreateParams);

        // when
        Mono<ResponseEntity<LineResponse>> response = 지하철_노선_조회_요청(createResponse);

        // then
        지하철_노선_응답됨(response, createResponse);
    }

    @DisplayName("지하철 노선을 수정한다.")
    @Test
    void updateLine() {
        // given
        String name = "신분당선";
        ResponseEntity<LineResponse> createResponse = 지하철_노선_등록되어_있음(lineCreateParams);

        // when
        Map<String, String> params = new HashMap<>();
        params.put("name", "구분당선");
        params.put("color", "bg-red-600");
        params.put("upStationId", 강남역.getId() + "");
        params.put("downStationId", 광교역.getId() + "");
        params.put("distance", 15 + "");
        Mono<ResponseEntity<Void>> response = 지하철_노선_수정_요청(createResponse, params);

        // then
        지하철_노선_수정됨(response);
    }

    @DisplayName("지하철 노선을 제거한다.")
    @Test
    void deleteLine() {
        // given
        ResponseEntity<LineResponse> createResponse = 지하철_노선_등록되어_있음(lineCreateParams);

        // when
        Mono<ResponseEntity<Void>> response = 지하철_노선_제거_요청(createResponse);

        // then
        지하철_노선_삭제됨(response);
    }

    public Mono<ResponseEntity<List<LineResponse>>> 지하철_노선_목록_조회_요청() {
        return webClient().get()
                .uri("/lines")
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntityList(LineResponse.class));
    }

    public Mono<ResponseEntity<Void>> 지하철_노선_수정_요청(ResponseEntity<LineResponse> response,
                                                             Map<String, String> params) {
        String uri = response.getHeaders().getFirst("Location");
        assert uri != null;

        return webClient().put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(params))
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }

    public Mono<ResponseEntity<Void>> 지하철_노선_제거_요청(ResponseEntity<LineResponse> response) {
        String uri = response.getHeaders().getFirst("Location");
        assert uri != null;

        return webClient().delete()
                .uri(uri)
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }

    public static void 지하철_노선_생성됨(Mono<ResponseEntity<LineResponse>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(r.getHeaders().getFirst("Location")).isNotBlank();
                })
                .verifyComplete();
    }

    public static void 지하철_노선_생성_실패됨(Mono<ResponseEntity<LineResponse>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }

    public static void 지하철_노선_목록_응답됨(Mono<ResponseEntity<List<LineResponse>>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                })
                .verifyComplete();
    }

    public static void 지하철_노선_응답됨(Mono<ResponseEntity<LineResponse>> response,
                                  ResponseEntity<LineResponse> createdResponse) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(r.getBody()).isNotNull();
                })
                .verifyComplete();
    }

    public static void 지하철_노선_목록_포함됨(Mono<ResponseEntity<List<LineResponse>>> response,
                                     List<ResponseEntity<LineResponse>> createdResponses) {
        List<Long> expectedLineIds = createdResponses.stream()
                .map(it -> Long.parseLong(Objects.requireNonNull(it.getHeaders().getFirst("Location")).split("/")[2]))
                .collect(Collectors.toList());

        StepVerifier.create(response)
                .assertNext(r -> {
                    List<Long> resultLineIds = Objects.requireNonNull(r.getBody()).stream()
                            .map(LineResponse::getId)
                            .collect(Collectors.toList());

                    assertThat(resultLineIds).containsAll(expectedLineIds);
                })
                .verifyComplete();
    }

    public static void 지하철_노선_수정됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                })
                .verifyComplete();
    }

    public static void 지하철_노선_삭제됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                })
                .verifyComplete();
    }
}
