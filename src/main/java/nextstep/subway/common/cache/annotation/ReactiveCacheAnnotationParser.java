package nextstep.subway.common.cache.annotation;

import nextstep.subway.common.cache.domain.ReactiveCacheEvictOperation;
import nextstep.subway.common.cache.domain.ReactiveCacheableOperation;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;

public class ReactiveCacheAnnotationParser implements CacheAnnotationParser, Serializable {
    private static final long serialVersionUID = -8941411813191989999L;

    private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<>(8);

    static {
        CACHE_OPERATION_ANNOTATIONS.add(ReactiveCacheable.class);
        CACHE_OPERATION_ANNOTATIONS.add(ReactiveCacheEvict.class);
        CACHE_OPERATION_ANNOTATIONS.add(ReactiveCaching.class);
    }

    @Override
    public boolean isCandidateClass(Class<?> targetClass) {
        return AnnotationUtils.isCandidateClass(targetClass, CACHE_OPERATION_ANNOTATIONS);
    }

    @Override
    public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
        return Collections.emptyList();
    }

    @Override
    public Collection<CacheOperation> parseCacheAnnotations(Method method) {
        List<CacheOperation> cacheOperations = new ArrayList<>();
        for (Annotation annotation : AnnotatedElementUtils.getAllMergedAnnotations(method, CACHE_OPERATION_ANNOTATIONS)) {
            addAnnotation(method, cacheOperations, annotation);
        }
        return cacheOperations;
    }

    private void addAnnotation(AnnotatedElement annotatedElement, List<CacheOperation> annotations, Annotation annotation) {
        if (annotation.annotationType()
                .isAssignableFrom(ReactiveCaching.class)) {
            addReactiveCaching(annotatedElement, annotations, (ReactiveCaching) annotation);
        }
        if (annotation.annotationType()
                .isAssignableFrom(ReactiveCacheable.class)) {
            annotations.add(parseReactiveCacheableAnnotation(annotatedElement, (ReactiveCacheable) annotation));
        }
        if (annotation.annotationType()
                .isAssignableFrom(ReactiveCacheEvict.class)) {
            annotations.add(parseReactiveCacheEvictAnnotation(annotatedElement, (ReactiveCacheEvict) annotation));
        }
    }

    private void addReactiveCaching(AnnotatedElement annotatedElement, List<CacheOperation> annotations, ReactiveCaching reactiveCaching) {
        ReactiveCacheable reactiveCacheable = reactiveCaching.cacheable();
        if (StringUtils.hasText(reactiveCacheable.value())) {
            annotations.add(parseReactiveCacheableAnnotation(annotatedElement, reactiveCacheable));
        }
        ReactiveCacheEvict[] reactiveCacheEvicts = reactiveCaching.evict();
        for (ReactiveCacheEvict reactiveCacheEvict : reactiveCacheEvicts) {
            annotations.add(parseReactiveCacheEvictAnnotation(annotatedElement, reactiveCacheEvict));
        }
    }

    private ReactiveCacheableOperation parseReactiveCacheableAnnotation(AnnotatedElement annotatedElement, ReactiveCacheable cacheable) {
        return ReactiveCacheableOperation.builder()
                .name(annotatedElement.toString())
                .cacheNames(cacheable.value())
                .key(cacheable.key())
                .build();
    }

    private ReactiveCacheEvictOperation parseReactiveCacheEvictAnnotation(AnnotatedElement annotatedElement, ReactiveCacheEvict cacheEvict) {
        return ReactiveCacheEvictOperation.builder()
                .name(annotatedElement.toString())
                .cacheNames(cacheEvict.value())
                .key(cacheEvict.key())
                .build();
    }
}
