package nextstep.subway.line.domain;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SectionRepository extends R2dbcRepository<Section, Long> {
    Flux<Section> findByLineId(Long lineId);

    Mono<Void> deleteByLineId(Long id);
}
