package nextstep.subway.station.handler;

import lombok.RequiredArgsConstructor;
import nextstep.subway.common.request.RequestConverter;
import nextstep.subway.common.request.RequestValidator;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.dto.StationRequest;
import nextstep.subway.station.dto.StationResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class StationHandler {
    private final StationService stationService;
    private final RequestValidator requestValidator;
    private final RequestConverter requestConverter;

    // @formatter:off
    public Mono<ServerResponse> createStation(ServerRequest request) {
        return request.bodyToMono(StationRequest.class)
                .doOnNext(requestValidator::validate)
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(stationService::saveStation)
                .flatMap(stationResponse -> ServerResponse.created(URI.create("/stations/" + stationResponse.getId()))
                        .body(BodyInserters.fromValue(stationResponse)));
    }
    // @formatter:on

    public Mono<ServerResponse> showStations() {
        return ServerResponse.ok()
                .body(stationService.findAllStations(), StationResponse.class);
    }

    // @formatter:off
    public Mono<ServerResponse> pagingStations(ServerRequest request) {
        Long id = request.queryParam("id")
                .map(Long::parseLong)
                .orElse(0L);
        Pageable pageable = requestConverter.pageable(request);
        return ServerResponse.ok()
                .body(stationService.findStations(id, pageable), StationResponse.class);
    }
    // @formatter:on

    public Mono<ServerResponse> deleteStation(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return ServerResponse.noContent()
                .build(stationService.deleteStationById(id));
    }
}
