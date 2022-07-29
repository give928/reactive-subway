package nextstep.subway.auth.application;

import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
@NoArgsConstructor
public class AuthorizationException extends RuntimeException {
    private static final long serialVersionUID = 1070562293693294378L;

    public AuthorizationException(String message) {
        super(message);
    }
}
