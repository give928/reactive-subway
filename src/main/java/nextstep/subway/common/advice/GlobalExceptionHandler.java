package nextstep.subway.common.advice;

import lombok.extern.slf4j.Slf4j;
import nextstep.subway.auth.application.AuthorizationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {"nextstep.subway"})
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Void> handleAuthorizationException(AuthorizationException e) {
        log.error("handleAuthorizationException", e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Void> handleRuntimeException(RuntimeException e) {
        log.error("handleRuntimeException", e);
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleException(Exception e) {
        log.error("handleException", e);
        return ResponseEntity.internalServerError().build();
    }
}
