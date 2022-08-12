package nextstep.subway.member.domain;

import lombok.*;
import nextstep.subway.auth.application.AuthorizationException;
import nextstep.subway.common.entity.BaseEntity;
import org.springframework.data.annotation.Id;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class Member extends BaseEntity {
    @Id
    private Long id;
    private String email;
    private String password;
    private Integer age;

    public Member update(Member member) {
        this.email = member.email;
        this.password = member.password;
        this.age = member.age;
        return this;
    }

    public Member checkPassword(String password) {
        if (!Objects.equals(this.password, password)) {
            throw new AuthorizationException();
        }
        return this;
    }
}
