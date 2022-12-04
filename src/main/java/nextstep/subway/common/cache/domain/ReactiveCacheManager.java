package nextstep.subway.common.cache.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nextstep.subway.common.cache.annotation.ReactiveCacheAnnotationParser;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
public class ReactiveCacheManager {
    private static final String CACHE_KEY_FORMAT = "%s:%s";
    private static final int MAX_REACTIVE_CACHEABLE_COUNT = 1;
    private static final CacheAnnotationParser CACHE_ANNOTATION_PARSER = new ReactiveCacheAnnotationParser();
    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final Function<String, Mono<Object>> get;
    private final BiFunction<String, Object, Mono<Object>> put;
    private final Function<String, Mono<Long>> evict;
    private final Function<String, Object> proceed;
    private final ObjectMapper objectMapper;
    private final Method method;
    private final TypeReference<?> typeReference;
    private final StandardEvaluationContext standardEvaluationContext;
    private final Collection<CacheOperation> cacheOperations;

    ReactiveCacheManager(Function<String, Mono<Object>> get, BiFunction<String, Object, Mono<Object>> put,
                         Function<String, Mono<Long>> evict, Function<String, Object> proceed,
                         ObjectMapper objectMapper, Method method, StandardEvaluationContext standardEvaluationContext) {
        this.get = get;
        this.put = put;
        this.evict = evict;
        this.proceed = proceed;
        this.objectMapper = objectMapper;
        this.method = method;
        this.typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
            }
        };
        this.standardEvaluationContext = standardEvaluationContext;
        this.cacheOperations = CACHE_ANNOTATION_PARSER.parseCacheAnnotations(method);
        validate();
    }

    public static ReactiveCacheManager of(Function<String, Mono<Object>> get,
                                          BiFunction<String, Object, Mono<Object>> put,
                                          Function<String, Mono<Long>> evict, Function<String, Object> proceed,
                                          Method method, StandardEvaluationContext standardEvaluationContext,
                                          ObjectMapper objectMapper) {
        return new ReactiveCacheManager(get, put, evict, proceed, objectMapper, method, standardEvaluationContext);
    }

    // @formatter:off
    private void validate() {
        if (!isReturnFlux() && !isReturnMono()) {
            throw new IllegalStateException("캐시 메서드의 반환 타입은 Mono<?> 또는 Flux<?> 만 지원합니다.");
        }
        if (cacheOperations.stream()
                .filter(cacheOperation -> cacheOperation.getClass()
                        .isAssignableFrom(ReactiveCacheableOperation.class))
                .count() > MAX_REACTIVE_CACHEABLE_COUNT) {
            throw new IllegalStateException("캐시 메서드의 @ReactiveCacheable 은 1개만 지원합니다.");
        }
    }
    // @formatter:on

    private boolean isReturnFlux() {
        return method.getReturnType()
                .isAssignableFrom(Flux.class);
    }

    private boolean isReturnMono() {
        return method.getReturnType()
                .isAssignableFrom(Mono.class);
    }

    public Object get() {
        if (isReturnFlux()) {
            return new ReactiveCacheFlux().get();
        }
        if (isReturnMono()) {
            return new ReactiveCacheMono().get();
        }
        return fallback("", proceed);
    }

    private Optional<String> getKey(CacheOperation cacheOperation) {
        Set<String> cacheNames = cacheOperation.getCacheNames();
        if (cacheNames.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(getKey(cacheNames.iterator()
                                          .next(), cacheOperation.getKey()));
    }

    private String getKey(String key, String id) {
        if (StringUtils.hasText(id) && (id.contains("#") || id.contains("'"))) {
            String idValue = String.valueOf(EXPRESSION_PARSER.parseExpression(id)
                                                    .getValue(standardEvaluationContext));
            return String.format(CACHE_KEY_FORMAT, key, idValue);
        }
        return key;
    }

    public Set<String> getKeys(CacheOperation cacheOperation) {
        Set<String> cacheNames = cacheOperation.getCacheNames();
        if (cacheNames.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> keys = new LinkedHashSet<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            keys.add(getKey(cacheName, cacheOperation.getKey()));
        }
        return keys;
    }

    private static <T> T fallback(String key, Function<String, Object> proceed) {
        log.debug("cache proceed key: {}", key);
        return (T) proceed.apply(key);
    }

    // @formatter:off
    private void put(String key, Object o) {
        log.debug("cache put key: {}", key);
        put.apply(key, o)
                .onErrorResume(throwable -> Mono.empty())
                .subscribe();
    }
    // @formatter:on

    // @formatter:off
    private void evict(CacheOperation cacheOperation) {
        Set<String> keys = getKeys(cacheOperation);
        if (keys.isEmpty()) {
            throw new IllegalStateException("@ReactiveCacheEvict 캐시 키를 설정해주세요.");
        }

        for (String key : keys) {
            log.debug("cache evict key: {}", key);
            evict.apply(key)
                    .onErrorResume(throwable -> Mono.empty())
                    .subscribe();
        }
    }
    // @formatter:on

    private interface ReactiveCache {
        Object get();
    }

    private class ReactiveCacheFlux implements ReactiveCache {
        public Flux<?> get() {
            if (cacheOperations == null || cacheOperations.isEmpty()) {
                return fallback("", proceed);
            }
            boolean callProceed = true;
            Flux<?> flux = Flux.just(1);
            for (CacheOperation cacheOperation : cacheOperations) {
                if (cacheOperation.getClass()
                        .isAssignableFrom(ReactiveCacheableOperation.class)) {
                    flux = flux.flatMap(o -> get(cacheOperation));
                    callProceed = false;
                }
                if (cacheOperation.getClass()
                        .isAssignableFrom(ReactiveCacheEvictOperation.class)) {
                    flux = flux.doOnNext(o -> evict(cacheOperation));
                }
            }
            if (callProceed) {
                return flux.flatMap(o -> fallback("", proceed));
            }
            return flux;
        }

        // @formatter:off
        private Flux<?> get(CacheOperation cacheOperation) {
            return getKey(cacheOperation).map(key -> {
                        log.debug("cache get key: {}", key);
                        return get.apply(key)
                                .flatMapMany(data -> toFlux((List<?>) data))
                                .onErrorResume(throwable -> Flux.defer(() -> fallback(key, proceed)))
                                .switchIfEmpty(Flux.defer(() -> fallbackPut(key, proceed)));
                    })
                    .orElseThrow(() -> new IllegalArgumentException("@ReactiveCacheable 캐시 키를 설정해주세요."));
        }
        // @formatter:on

        // @formatter:off
        private Flux<Object> toFlux(List<?> data) {
            return Flux.fromStream(data.stream()
                                           .map(item -> objectMapper.convertValue(item, typeReference)));
        }
        // @formatter:on

        // @formatter:off
        private Flux<?> fallbackPut(String key, Function<String, Object> fallback) {
            return ReactiveCacheManager.<Flux<?>>fallback(key, fallback)
                    .collectList()
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(o -> put(key, o))
                    .flatMapMany(Flux::fromIterable);
        }
        // @formatter:on
    }

    private class ReactiveCacheMono implements ReactiveCache {
        public Mono<?> get() {
            if (cacheOperations == null || cacheOperations.isEmpty()) {
                return fallback("", proceed);
            }
            boolean callProceed = true;
            Mono<?> mono = Mono.just(1);
            for (CacheOperation cacheOperation : cacheOperations) {
                if (cacheOperation.getClass()
                        .isAssignableFrom(ReactiveCacheableOperation.class)) {
                    mono = mono.flatMap(o -> get(cacheOperation));
                    callProceed = false;
                }
                if (cacheOperation.getClass()
                        .isAssignableFrom(ReactiveCacheEvictOperation.class)) {
                    mono = mono.doOnNext(o -> evict(cacheOperation));
                }
            }
            if (callProceed) {
                return mono.flatMap(o -> fallback("", proceed));
            }
            return mono;
        }

        // @formatter:off
        public Mono<?> get(CacheOperation cacheOperation) {
            return getKey(cacheOperation).map(key -> {
                        log.debug("cache get key: {}", key);
                        return get.apply(key)
                                .onErrorResume(throwable -> Mono.defer(() -> fallback(key, proceed)))
                                .switchIfEmpty(Mono.defer(() -> fallbackPut(key, proceed)))
                                .map(item -> objectMapper.convertValue(item, typeReference));
                    })
                    .orElseThrow(() -> new IllegalArgumentException("@ReactiveCacheable 캐시 키를 설정해주세요."));
        }
        // @formatter:on

        // @formatter:off
        private Mono<?> fallbackPut(String key, Function<String, Object> fallback) {
            return ReactiveCacheManager.<Mono<?>>fallback(key, fallback)
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext(o -> put(key, o));
        }
        // @formatter:on
    }
}
