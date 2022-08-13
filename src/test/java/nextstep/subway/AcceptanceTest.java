package nextstep.subway;

import io.restassured.RestAssured;
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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.HashMap;
import java.util.Map;

@ActiveProfiles({"test"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AcceptanceTest {
    @Value("${local.server.port}")
    protected int port;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @BeforeEach
    public void setUp() {
        if (RestAssured.port == RestAssured.UNDEFINED_PORT) {
            RestAssured.port = port;
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
        Map<String, String> params = new HashMap<>();
        params.put("name", name);

        return webClient().post()
                .uri("/stations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(params))
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode() == HttpStatus.CREATED) {
                        return clientResponse.toEntity(StationResponse.class);
                    }
                    return Mono.just(ResponseEntity.status(clientResponse.statusCode()).build());
                });
    }
}
