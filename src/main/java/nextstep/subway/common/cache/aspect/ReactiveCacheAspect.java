package nextstep.subway.common.cache.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nextstep.subway.common.cache.domain.CacheRepository;
import nextstep.subway.common.cache.domain.ReactiveCacheManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.function.Function;

@Component
@Aspect
@Order(2)
@Slf4j
public class ReactiveCacheAspect {
    private final CacheRepository<String, Object> cacheRepository;
    private final ObjectMapper objectMapper;

    public ReactiveCacheAspect(CacheRepository<String, Object> cacheRepository, ObjectMapper objectMapper) {
        this.cacheRepository = cacheRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(nextstep.subway.common.cache.annotation.ReactiveCacheable)" +
            " || @annotation(nextstep.subway.common.cache.annotation.ReactiveCacheEvict)" +
            " || @annotation(nextstep.subway.common.cache.annotation.ReactiveCaching)")
    public Object doAround(ProceedingJoinPoint joinPoint) {
        Function<String, Object> proceed = getProceed(joinPoint);
        Method method = getMethod(joinPoint);
        StandardEvaluationContext standardEvaluationContext = createStandardEvaluationContext(joinPoint);
        return ReactiveCacheManager.of(cacheRepository::get, cacheRepository::put, cacheRepository::evict, proceed,
                                       method, standardEvaluationContext, objectMapper)
                .get();
    }

    private static Function<String, Object> getProceed(ProceedingJoinPoint joinPoint) {
        return key -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private static Method getMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        return methodSignature.getMethod();
    }

    private StandardEvaluationContext createStandardEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = codeSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        return context;
    }
}
