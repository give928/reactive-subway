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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
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

    private final ObjectMapper objectMapper;

    public LoggableAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(nextstep.subway.common.annotation.Loggable) && @annotation(loggable)")
    public Object doAround(ProceedingJoinPoint joinPoint, Loggable loggable) {
        return Mono.fromCallable(() -> UUID.randomUUID().toString())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(traceId -> {
                    try {
                        doBefore(traceId, joinPoint, loggable);
                        return ((Mono<?>) joinPoint.proceed())
                                .doOnSuccess(result -> doReturn(traceId, joinPoint, loggable, result))
                                .doOnError(throwable -> doThrowing(traceId, joinPoint, throwable))
                                .doFinally(signalType -> doAfter(traceId, signalType));
                    } catch (Throwable throwable) {
                        doThrowing(traceId, joinPoint, throwable);
                    } finally {
                        doAfter(traceId);
                    }
                    return Mono.defer(() -> Mono.error(RuntimeException::new));
                });
    }

    public void doBefore(String traceId, JoinPoint joinPoint, Loggable loggable) {
        logBefore(traceId, joinPoint);
        logBeforeJson(traceId, joinPoint, loggable);
    }

    private void logBefore(String traceId, JoinPoint joinPoint) {
        out.info("[{}][before] {}", traceId, joinPoint.getSignature());
        CodeSignature methodSignature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        IntStream.range(0, parameterNames.length)
                .forEach(i -> out.info("[{}][before] {}: {}", traceId, parameterNames[i], convertJsonString(traceId, args[i])));
    }

    private void logBeforeJson(String traceId, JoinPoint joinPoint, Loggable loggable) {
        if (loggable.json()) {
            json.info(makeArgumentsMarker(traceId, joinPoint), "arguments");
        }
    }

    private LogstashMarker makeArgumentsMarker(String traceId, JoinPoint joinPoint) {
        CodeSignature methodSignature = (CodeSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        LogstashMarker logstashMarker = Markers.empty();
        IntStream.range(0, parameterNames.length)
                .forEach(i -> logstashMarker.and(makeArgumentMarker(traceId, parameterNames[i], args[i])));
        return logstashMarker;
    }

    private LogstashMarker makeArgumentMarker(String traceId, String parameterName, Object arg) {
        if (arg.getClass().getSimpleName().endsWith("List")) {
            return appendRaw(parameterName, convertJsonEmptyString(traceId, arg));
        }
        if (arg.getClass().getSimpleName().endsWith("Map")) {
            return append(parameterName, appendEntries((Map<?, ?>) arg));
        }
        return append(parameterName, arg);
    }

    private String convertJsonEmptyString(String traceId, Object object) {
        String jsonString = convertJsonString(traceId, object);
        if (jsonString != null) {
            return jsonString;
        }
        return "";
    }

    private String convertJsonString(String traceId, Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            error.error("[{}][exception]", traceId, e);
        }
        return null;
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
        String resultJson = convertJsonString(traceId, result);
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

    public void doThrowing(String traceId, JoinPoint joinPoint, Throwable throwable) {
        error.error("[{}][exception] {}", traceId, joinPoint.getSignature(), throwable);
    }

    public void doAfter(String traceId) {
        if (out.isDebugEnabled()) {
            out.debug("[{}][after]", traceId);
        }
    }

    public void doAfter(String traceId, SignalType signalType) {
        if (out.isDebugEnabled()) {
            out.debug("[{}][after] {}", traceId, signalType);
        }
    }
}
