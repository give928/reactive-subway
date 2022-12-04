package nextstep.subway.common.cache.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisValueRepository<V> implements CacheRepository<String, V> {
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisValueRepository(ReactiveStringRedisTemplate reactiveStringRedisTemplate,
                                ObjectMapper objectMapper) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    // @formatter:off
    @Override
    public Mono<V> get(String key) {
        return reactiveStringRedisTemplate.opsForValue()
                .get(key)
                .map(o -> {
                    try {
                        return objectMapper.readValue(o, new TypeReference<>() {
                        });
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }
    // @formatter:on

    // @formatter:off
    @Override
    public Mono<V> put(String key, V value) {
        try {
            return reactiveStringRedisTemplate.opsForValue()
                    .set(key, objectMapper.writeValueAsString(value))
                    .thenReturn(value);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
    // @formatter:on

    // @formatter:off
    @Override
    public Mono<V> put(String key, V value, Duration timeout) {
        try {
            return reactiveStringRedisTemplate.opsForValue()
                    .set(key, objectMapper.writeValueAsString(value), timeout)
                    .thenReturn(value);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }
    // @formatter:on

    @Override
    public Mono<Long> evict(String key) {
        return reactiveStringRedisTemplate.delete(key);
    }
}
