package nextstep.subway;

import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.station.dto.StationResponse;
import nextstep.subway.utils.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.Map;
import java.util.Objects;

@ActiveProfiles({"test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AcceptanceTest {
    @Value("${local.server.port}")
    protected int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @BeforeEach
    public void setUp() {
        if (!databaseCleanup.isInitProperties()) {
            databaseCleanup.afterPropertiesSet();
        }

        databaseCleanup.execute();
    }

    protected WebClient webClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.newConnection().compress(true)))
                .build();
    }

    public ResponseEntity<StationResponse> 지하철역_등록되어_있음(String name) {
        return 지하철역_생성_요청(name).block();
    }

    public Mono<ResponseEntity<StationResponse>> 지하철역_생성_요청(String name) {
        return webClient().post()
                .uri("/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of("name", name)))
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.CREATED) {
                        return clientResponse.toEntity(StationResponse.class);
                    }
                    return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
                });
    }

    public ResponseEntity<Void> 회원_등록되어_있음(String email, String password, Integer age) {
        return 회원_생성_요청(email, password, age).block();
    }

    public Mono<ResponseEntity<Void>> 회원_생성_요청(String email, String password, Integer age) {
        return webClient()
                .post()
                .uri("/members")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of("email", email, "password", password, "age", age)))
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }

    public TokenResponse 로그인_되어_있음(String email, String password) {
        Mono<ResponseEntity<TokenResponse>> response = 로그인_요청(email, password);
        return Objects.requireNonNull(response.block()).getBody();
    }

    public Mono<ResponseEntity<TokenResponse>> 로그인_요청(String email, String password) {
        return webClient().post()
                .uri("/login/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(Map.of("email", email, "password", password)))
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.OK) {
                        return clientResponse.toEntity(TokenResponse.class);
                    }
                    return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
                });
    }

    public ResponseEntity<LineResponse> 지하철_노선_등록되어_있음(Map<String, String> params) {
        return 지하철_노선_생성_요청(params).block();
    }

    public Mono<ResponseEntity<LineResponse>> 지하철_노선_생성_요청(Map<String, String> params) {
        return webClient().post()
                .uri("/lines")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(params))
                .exchangeToMono(clientResponse -> clientResponse.toEntity(LineResponse.class));
    }

    public Mono<ResponseEntity<LineResponse>> 지하철_노선_조회_요청(LineResponse response) {
        return webClient().get()
                .uri("/lines/{lineId}", response.getId())
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(LineResponse.class));
    }

    public Mono<ResponseEntity<LineResponse>> 지하철_노선_조회_요청(ResponseEntity<LineResponse> response) {
        String uri = response.getHeaders().getFirst("Location");
        assert uri != null;

        return webClient().get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(clientResponse -> clientResponse.toEntity(LineResponse.class));
    }

    public Mono<ResponseEntity<Void>> 지하철_노선에_지하철역_등록_요청(LineResponse line, StationResponse upStation,
                                                         StationResponse downStation, int distance) {
        return webClient().post()
                .uri("/lines/{lineId}/sections", line.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(
                        Map.of("upStationId", upStation.getId(), "downStationId", downStation.getId(), "distance",
                               distance)))
                .exchangeToMono(ClientResponse::toBodilessEntity);
    }
}
