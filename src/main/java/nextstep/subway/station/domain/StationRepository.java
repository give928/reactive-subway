package nextstep.subway.station.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

public interface StationRepository extends R2dbcRepository<Station, Long> {
//    @Query("SELECT * FROM station as s WHERE s.id > :id")
    Flux<Station> findByIdGreaterThan(@Param("id") Long id, Pageable pageable);
}
