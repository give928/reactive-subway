package nextstep.subway.line.acceptance;

import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 노선에 역 등록 관련 기능")
public class LineSectionAcceptanceTest extends AcceptanceTest {
    private LineResponse 신분당선;
    private StationResponse 강남역;
    private StationResponse 양재역;
    private StationResponse 정자역;
    private StationResponse 광교역;

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
    }

    @DisplayName("지하철 노선에 역을 등록한다.")
    @Test
    void addLineSection() {
        // when
        지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, 3).block();

        // then
        Mono<ResponseEntity<LineResponse>> response = 지하철_노선_조회_요청(신분당선);
        지하철_노선에_지하철역_등록됨(response);
        지하철_노선에_지하철역_순서_정렬됨(response, Arrays.asList(강남역, 양재역, 광교역));
    }

    @DisplayName("지하철 노선에 여러개의 역을 순서 상관 없이 등록한다.")
    @Test
    void addLineSection2() {
        // when
        지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, 2).block();
        지하철_노선에_지하철역_등록_요청(신분당선, 정자역, 강남역, 5).block();

        // then
        Mono<ResponseEntity<LineResponse>> response = 지하철_노선_조회_요청(신분당선);
        지하철_노선에_지하철역_등록됨(response);
        지하철_노선에_지하철역_순서_정렬됨(response, Arrays.asList(정자역, 강남역, 양재역, 광교역));
    }

    @DisplayName("지하철 노선에 이미 등록되어있는 역을 등록한다.")
    @Test
    void addLineSectionWithSameStation() {
        // when
        Mono<ResponseEntity<Void>> response = 지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 광교역, 3);

        // then
        지하철_노선에_지하철역_등록_실패됨(response);
    }

    @DisplayName("지하철 노선에 등록되지 않은 역을 기준으로 등록한다.")
    @Test
    void addLineSectionWithNoStation() {
        // when
        Mono<ResponseEntity<Void>> response = 지하철_노선에_지하철역_등록_요청(신분당선, 정자역, 양재역, 3);

        // then
        지하철_노선에_지하철역_등록_실패됨(response);
    }

    @DisplayName("지하철 노선에 등록된 지하철역을 제외한다.")
    @Test
    void removeLineSection1() {
        // given
        지하철_노선에_지하철역_등록_요청(신분당선, 강남역, 양재역, 2).block();
        지하철_노선에_지하철역_등록_요청(신분당선, 양재역, 정자역, 2).block();

        // when
        Mono<ResponseEntity<Void>> removeResponse = 지하철_노선에_지하철역_제외_요청(신분당선, 양재역);

        // then
        지하철_노선에_지하철역_제외됨(removeResponse);
        Mono<ResponseEntity<LineResponse>> response = 지하철_노선_조회_요청(신분당선);
        지하철_노선에_지하철역_순서_정렬됨(response, Arrays.asList(강남역, 정자역, 광교역));
    }

    @DisplayName("지하철 노선에 등록된 지하철역이 두개일 때 한 역을 제외한다.")
    @Test
    void removeLineSection2() {
        // when
        Mono<ResponseEntity<Void>> removeResponse = 지하철_노선에_지하철역_제외_요청(신분당선, 강남역);

        // then
        지하철_노선에_지하철역_제외_실패됨(removeResponse);
    }

    public static void 지하철_노선에_지하철역_등록됨(Mono<ResponseEntity<LineResponse>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                })
                .verifyComplete();
    }

    public static void 지하철_노선에_지하철역_등록_실패됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }

    public static void 지하철_노선에_지하철역_순서_정렬됨(Mono<ResponseEntity<LineResponse>> response, List<StationResponse> expectedStations) {
        List<Long> expectedStationIds = expectedStations.stream()
                .map(StationResponse::getId)
                .collect(Collectors.toList());

        StepVerifier.create(response)
                .assertNext(r -> {
                    List<Long> stationIds = Objects.requireNonNull(r.getBody()).getStations().stream()
                            .map(StationResponse::getId)
                            .collect(Collectors.toList());

                    assertThat(stationIds).containsExactlyElementsOf(expectedStationIds);
                })
                .verifyComplete();
    }

    public Mono<ResponseEntity<Void>> 지하철_노선에_지하철역_제외_요청(LineResponse line, StationResponse station) {
        return webClient().delete()
                .uri("/lines/{lineId}/sections?stationId={stationId}", line.getId(), station.getId())
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }

    public static void 지하철_노선에_지하철역_제외됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
                })
                .verifyComplete();
    }

    public static void 지하철_노선에_지하철역_제외_실패됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }
}
