package nextstep.subway.station.application;

import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import nextstep.subway.station.dto.StationRequest;
import nextstep.subway.station.dto.StationResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StationService {
    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    // @formatter:off
    @Transactional
    public Mono<StationResponse> saveStation(StationRequest stationRequest) {
        return stationRepository.save(stationRequest.toStation())
                .onErrorMap(RuntimeException::new)
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .map(StationResponse::of);
    }
    // @formatter:on

    @Transactional(readOnly = true)
    public Flux<StationResponse> findAllStations() {
        return findAll().map(StationResponse::of);
    }

    public Flux<Station> findAll() {
        return stationRepository.findAll();
    }

    public Flux<Station> findAllById(Set<Long> stationIds) {
        return stationRepository.findAllById(stationIds);
    }

    public Flux<StationResponse> findStations(Long id, Pageable pageable) {
        return stationRepository.findByIdGreaterThan(id, pageable)
                .map(StationResponse::of);
    }

    @Transactional
    public Mono<Void> deleteStationById(Long id) {
        return stationRepository.deleteById(id);
    }

    public Mono<Station> findById(Long id) {
        return stationRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())));
    }
}
