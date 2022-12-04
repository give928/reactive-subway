package nextstep.subway.common.cache.domain;

import org.springframework.cache.interceptor.CacheOperation;

public class ReactiveCacheEvictOperation extends CacheOperation {
    protected ReactiveCacheEvictOperation(Builder b) {
        super(b);
    }

    public static ReactiveCacheEvictOperation.Builder builder() {
        return new ReactiveCacheEvictOperation.Builder();
    }

    public static class Builder extends CacheOperation.Builder {
        @Override
        public ReactiveCacheEvictOperation build() {
            return new ReactiveCacheEvictOperation(this);
        }

        public ReactiveCacheEvictOperation.Builder name(String name) {
            setName(name);
            return this;
        }

        public ReactiveCacheEvictOperation.Builder cacheNames(String... cacheNames) {
            setCacheNames(cacheNames);
            return this;
        }

        public ReactiveCacheEvictOperation.Builder key(String key) {
            setKey(key);
            return this;
        }
    }
}
