package nextstep.subway.favorite.domain;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface FavoriteRepository extends R2dbcRepository<Favorite, Long> {
    Flux<Favorite> findByMemberId(Long memberId);
}
