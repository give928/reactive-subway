package nextstep.subway.auth.domain;

import lombok.Getter;

@Getter
public class LoginMember {
    private final Long id;
    private final String email;
    private final Integer age;

    private LoginMember(Long id, String email, Integer age) {
        this.id = id;
        this.email = email;
        this.age = age;
    }

    public static LoginMember of(Long id, String email, Integer age) {
        return new LoginMember(id, email, age);
    }
}
