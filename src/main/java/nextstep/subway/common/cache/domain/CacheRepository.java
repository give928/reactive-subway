package nextstep.subway.common.cache.domain;

import reactor.core.publisher.Mono;

import java.time.Duration;

public interface CacheRepository<K, V> {
    Mono<V> get(K id);

    Mono<V> put(K id, V data);
    Mono<V> put(K id, V data, Duration timeout);

    Mono<Long> evict(K id);
}
