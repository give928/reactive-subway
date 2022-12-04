package nextstep.subway.common.cache.domain;

import org.springframework.cache.interceptor.CacheOperation;

public class ReactiveCacheableOperation extends CacheOperation {
    protected ReactiveCacheableOperation(Builder b) {
        super(b);
    }

    public static ReactiveCacheableOperation.Builder builder() {
        return new ReactiveCacheableOperation.Builder();
    }

    public static class Builder extends CacheOperation.Builder {
        @Override
        public ReactiveCacheableOperation build() {
            return new ReactiveCacheableOperation(this);
        }

        public ReactiveCacheableOperation.Builder name(String name) {
            setName(name);
            return this;
        }

        public ReactiveCacheableOperation.Builder cacheNames(String... cacheNames) {
            setCacheNames(cacheNames);
            return this;
        }

        public ReactiveCacheableOperation.Builder key(String key) {
            setKey(key);
            return this;
        }
    }
}
