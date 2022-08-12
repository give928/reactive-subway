package nextstep.subway.line.domain;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface LineRepository extends R2dbcRepository<Line, Long> {
}
