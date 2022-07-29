package nextstep.subway.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginMember {
    private final Long id;
    private final String email;
    private final Integer age;
}
