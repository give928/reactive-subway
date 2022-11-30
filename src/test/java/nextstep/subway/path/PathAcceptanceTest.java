package nextstep.subway.path;

import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.map.dto.PathResponse;
import nextstep.subway.station.dto.StationResponse;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@DisplayName("지하철 경로 조회")
public class PathAcceptanceTest extends AcceptanceTest {
    private LineResponse 신분당선;
    private LineResponse 이호선;
    private LineResponse 삼호선;
    private StationResponse 강남역;
    private StationResponse 양재역;
    private StationResponse 교대역;
    private StationResponse 남부터미널역;

    /**
     * 교대역    --- *2호선* ---   강남역
     * |                        |
     * *3호선*                   *신분당선*
     * |                        |
     * 남부터미널역  --- *3호선* ---   양재
     */
    @BeforeEach
    public void setUp() {
        super.setUp();

        강남역 = 지하철역_등록되어_있음("강남역").getBody();
        양재역 = 지하철역_등록되어_있음("양재역").getBody();
        교대역 = 지하철역_등록되어_있음("교대역").getBody();
        남부터미널역 = 지하철역_등록되어_있음("남부터미널역").getBody();

        신분당선 = 지하철_노선_등록되어_있음("신분당선", "bg-red-600", 강남역, 양재역, 10);
        이호선 = 지하철_노선_등록되어_있음("이호선", "bg-red-600", 교대역, 강남역, 10);
        삼호선 = 지하철_노선_등록되어_있음("삼호선", "bg-red-600", 교대역, 양재역, 5);

        지하철_노선에_지하철역_등록되어_있음(삼호선, 교대역, 남부터미널역, 3);
    }

    @DisplayName("두 역의 최단 거리 경로를 조회한다.")
    @Test
    void findPathByDistance() {
        //when
        Mono<ResponseEntity<PathResponse>> response = 거리_경로_조회_요청(3L, 2L);

        //then
        적절한_경로를_응답(response, Lists.newArrayList(교대역, 남부터미널역, 양재역));
        총_거리와_소요_시간을_함께_응답함(response, 5);
    }

    private LineResponse 지하철_노선_등록되어_있음(String name, String color, StationResponse upStation,
                                        StationResponse downStation, int distance) {
        Map<String, String> lineCreateParams = new HashMap<>();
        lineCreateParams.put("name", name);
        lineCreateParams.put("color", color);
        lineCreateParams.put("upStationId", upStation.getId() + "");
        lineCreateParams.put("downStationId", downStation.getId() + "");
        lineCreateParams.put("distance", distance + "");
        return 지하철_노선_등록되어_있음(lineCreateParams).getBody();
    }

    private void 지하철_노선에_지하철역_등록되어_있음(LineResponse line, StationResponse upStation, StationResponse downStation,
                                      int distance) {
        지하철_노선에_지하철역_등록_요청(line, upStation, downStation, distance).block();
    }

    public Mono<ResponseEntity<PathResponse>> 거리_경로_조회_요청(long source, long target) {
        return webClient().get()
                .uri("/paths?source={sourceId}&target={targetId}", source, target)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(PathResponse.class));
    }

    public static void 적절한_경로를_응답(Mono<ResponseEntity<PathResponse>> response,
                                  ArrayList<StationResponse> expectedPath) {
        List<Long> expectedPathIds = expectedPath.stream()
                .map(StationResponse::getId)
                .collect(Collectors.toList());

        StepVerifier.create(response)
                .assertNext(r -> {
                    PathResponse pathResponse = r.getBody();

                    assertThat(pathResponse).isNotNull();

                    List<Long> stationIds = pathResponse.getStations().stream()
                            .map(StationResponse::getId)
                            .collect(Collectors.toList());

                    assertThat(stationIds).containsExactlyElementsOf(expectedPathIds);
                })
                .verifyComplete();
    }

    public static void 총_거리와_소요_시간을_함께_응답함(Mono<ResponseEntity<PathResponse>> response, int totalDistance) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    PathResponse pathResponse = r.getBody();
                    assertThat(pathResponse).isNotNull();
                    assertThat(pathResponse.getDistance()).isEqualTo(totalDistance);
                })
                .verifyComplete();
    }
}
