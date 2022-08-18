package nextstep.subway.common.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import nextstep.subway.common.annotation.Loggable;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static net.logstash.logback.marker.Markers.*;

@Component
@Aspect
@Order(1)
public class LoggableAspect {
    private static final Logger out = LoggerFactory.getLogger("out");
    private static final Logger error = LoggerFactory.getLogger("error");
    private static final Logger json = LoggerFactory.getLogger("json");
    private static final String TRACE_ID = "traceId";
    private static final String TRACE_SIGNATURE = "signature";
    private static final String MONO = "Mono";
    private static final String FLUX = "Flux";

    private final ObjectMapper objectMapper;

    public LoggableAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(nextstep.subway.common.annotation.Loggable) && @annotation(loggable)")
    public Object doAround(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        String returnType = getReturnType(joinPoint);
        if (MONO.equals(returnType)) {
            return mono(joinPoint, loggable);
        }
        if (FLUX.equals(returnType)) {
            return flux(joinPoint, loggable);
        }
        return object(joinPoint, loggable);
    }

    // @formatter:off
    private Mono<?> mono(ProceedingJoinPoint joinPoint, Loggable loggable) {
        return Mono.fromCallable(() -> UUID.randomUUID().toString())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(traceId -> {
                    try {
                        doBefore(traceId, joinPoint, loggable);
                        return ((Mono<?>) joinPoint.proceed()).doOnNext(result -> doReturn(traceId, joinPoint, loggable, result));
                    } catch (Throwable throwable) {
                        doThrowing(joinPoint, throwable);
                    } finally {
                        doAfter();
                    }
                    return Mono.defer(() -> Mono.error(RuntimeException::new));
                });
    }
    // @formatter:on

    // @formatter:off
    private Flux<?> flux(ProceedingJoinPoint joinPoint, Loggable loggable) {
        return Mono.fromCallable(() -> UUID.randomUUID().toString())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(traceId -> {
                    try {
                        doBefore(traceId, joinPoint, loggable);
                        return ((Flux<?>) joinPoint.proceed()).doOnNext(result -> doReturn(traceId, joinPoint, loggable, result));
                    } catch (Throwable throwable) {
                        doThrowing(joinPoint, throwable);
                    } finally {
                        doAfter();
                    }
                    return Flux.defer(() -> Flux.error(RuntimeException::new));
                });
    }
    // @formatter:on

    // @formatter:off
    private Object object(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        Mono<String> uuid = Mono.fromCallable(() -> UUID.randomUUID().toString())
                .subscribeOn(Schedulers.boundedElastic());
        try {
            uuid = uuid.doOnNext(traceId -> doBefore(traceId, joinPoint, loggable));
            Object result = joinPoint.proceed();
            uuid = uuid.doOnNext(traceId -> doReturn(traceId, joinPoint, loggable, result));
            return result;
        } catch (Throwable throwable) {
            uuid = uuid.doOnNext(traceId -> doThrowing(joinPoint, throwable));
            throw throwable;
        } finally {
            uuid.doOnNext(traceId -> doAfter())
                    .subscribe();
        }
    }
    // @formatter:on

    private String getReturnType(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().toString().split(" ")[0];
    }

    public void doBefore(String traceId, JoinPoint joinPoint, Loggable loggable) {
        initTrace(traceId, joinPoint);
        logBefore(joinPoint);
        logBeforeJson(joinPoint, loggable);
    }

    private void initTrace(String traceId, JoinPoint joinPoint) {
        MDC.put(TRACE_ID, traceId);
        MDC.put(TRACE_SIGNATURE, joinPoint.getSignature().toString());
    }

    private void logBefore(JoinPoint joinPoint) {
        out.info("[{}][before] {}", MDC.get(TRACE_ID), MDC.get(TRACE_SIGNATURE));
        CodeSignature methodSignature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        IntStream.range(0, parameterNames.length)
                .forEach(i -> out.info("[{}][before] {}: {}", MDC.get(TRACE_ID), parameterNames[i], convertJsonString(args[i])));
    }

    private void logBeforeJson(JoinPoint joinPoint, Loggable loggable) {
        if (loggable.json()) {
            json.info(makeArgumentsMarker(joinPoint), "arguments");
        }
    }

    public void doReturn(String traceId, JoinPoint joinPoint, Loggable loggable, Object result) {
        logReturn(traceId, joinPoint, result);
        logReturnJson(loggable, result);
    }

    private void logReturn(String traceId, JoinPoint joinPoint, Object result) {
        Signature signature = joinPoint.getSignature();
        if (result == null) {
            out.info("[{}][return] {}", traceId, signature);
            return;
        }
        String resultJson = convertJsonString(result);
        out.info("[{}][return] {}: {}", traceId, signature, resultJson);
    }

    private void logReturnJson(Loggable loggable, Object result) {
        if (!loggable.json()) {
            return;
        }
        if (result == null) {
            return;
        }
        logReturnJson(result);
    }

    private void logReturnJson(Object result) {
        json.info(append(result.getClass().getSimpleName(), result), "return");
    }

    public void doThrowing(JoinPoint joinPoint, Throwable throwable) {
        error.error("[{}][exception] {}", MDC.get(TRACE_ID), joinPoint.getSignature(), throwable);
    }

    public void doAfter() {
        MDC.clear();
    }

    private LogstashMarker makeArgumentsMarker(JoinPoint joinPoint) {
        CodeSignature methodSignature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        LogstashMarker logstashMarker = Markers.empty();
        IntStream.range(0, parameterNames.length)
                .forEach(i -> logstashMarker.and(makeArgumentMarker(parameterNames[i], args[i])));
        return logstashMarker;
    }

    private LogstashMarker makeArgumentMarker(String parameterName, Object arg) {
        if (arg.getClass().getSimpleName().endsWith("List")) {
            return appendRaw(parameterName, convertJsonEmptyString(arg));
        }
        if (arg.getClass().getSimpleName().endsWith("Map")) {
            return append(parameterName, appendEntries((Map<?, ?>) arg));
        }
        return append(parameterName, arg);
    }

    private String convertJsonEmptyString(Object object) {
        String jsonString = convertJsonString(object);
        if (jsonString != null) {
            return jsonString;
        }
        return "";
    }

    private String convertJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            error.error("[{}][exception]", MDC.get(TRACE_ID), e);
        }
        return null;
    }
}
