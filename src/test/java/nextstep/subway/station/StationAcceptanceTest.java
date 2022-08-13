package nextstep.subway.station;

import nextstep.subway.AcceptanceTest;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철역 관련 기능")
public class StationAcceptanceTest extends AcceptanceTest {
    private static final String 강남역 = "강남역";
    private static final String 역삼역 = "역삼역";

    @DisplayName("지하철역을 생성한다.")
    @Test
    void createStation() {
        // when
        Mono<ResponseEntity<StationResponse>> response = 지하철역_생성_요청(강남역);

        // then
        지하철역_생성됨(response);
    }

    @DisplayName("기존에 존재하는 지하철역 이름으로 지하철역을 생성한다.")
    @Test
    void createStationWithDuplicateName() {
        //given
        지하철역_등록되어_있음(강남역);

        // when
        Mono<ResponseEntity<StationResponse>> response = 지하철역_생성_요청(강남역);

        // then
        지하철역_생성_실패됨(response);
    }

    @DisplayName("지하철역을 조회한다.")
    @Test
    void getStations() {
        // given
        ResponseEntity<StationResponse> createResponse1 = 지하철역_등록되어_있음(강남역);
        ResponseEntity<StationResponse> createResponse2 = 지하철역_등록되어_있음(역삼역);

        // when
        Mono<ResponseEntity<List<StationResponse>>> response = 지하철역_목록_조회_요청();

        // then
        지하철역_목록_포함됨(response, Arrays.asList(createResponse1, createResponse2));
    }

    @DisplayName("지하철역을 제거한다.")
    @Test
    void deleteStation() {
        // given
        ResponseEntity<StationResponse> createResponse = 지하철역_등록되어_있음(강남역);

        // when
        Mono<ResponseEntity<Void>> response = 지하철역_제거_요청(createResponse);

        // then
        지하철역_삭제됨(response);
    }

    public Mono<ResponseEntity<List<StationResponse>>> 지하철역_목록_조회_요청() {
        return webClient().get()
                .uri("/stations")
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntityList(StationResponse.class));
    }

    public Mono<ResponseEntity<Void>> 지하철역_제거_요청(ResponseEntity<StationResponse> response) {
        String uri = response.getHeaders().getLocation().getPath();

        return webClient().delete()
                .uri(uri)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(Void.class));
    }

    public static void 지하철역_생성됨(Mono<ResponseEntity<StationResponse>> response) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                    assertThat(r.getHeaders().getLocation()).isNotNull();
                })
                .verifyComplete();
    }

    public static void 지하철역_생성_실패됨(Mono<ResponseEntity<StationResponse>> response) {
        StepVerifier.create(response)
                .assertNext(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verifyComplete();
    }

    public static void 지하철역_삭제됨(Mono<ResponseEntity<Void>> response) {
        StepVerifier.create(response)
                .assertNext(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT))
                .verifyComplete();

    }

    public static void 지하철역_목록_포함됨(Mono<ResponseEntity<List<StationResponse>>> response,
                                   List<ResponseEntity<StationResponse>> createdResponses) {
        StepVerifier.create(response)
                .assertNext(r -> {
                    assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);

                    List<Long> expectedLineIds = createdResponses.stream()
                            .map(it -> Long.parseLong(
                                    Objects.requireNonNull(it.getHeaders().getLocation()).getPath().split("/")[2]))
                            .sorted()
                            .collect(Collectors.toList());

                    List<Long> resultLineIds = Optional.ofNullable(r.getBody())
                            .orElse(Collections.emptyList())
                            .stream()
                            .map(StationResponse::getId)
                            .sorted()
                            .collect(Collectors.toList());

                    assertThat(resultLineIds).isEqualTo(expectedLineIds);
                })
                .verifyComplete();
    }
}
