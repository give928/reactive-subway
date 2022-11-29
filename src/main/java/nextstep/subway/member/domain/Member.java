package nextstep.subway.member.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.auth.application.AuthorizationException;
import nextstep.subway.common.entity.BaseEntity;
import org.springframework.data.annotation.Id;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends BaseEntity {
    @Id
    private Long id;
    private String email;
    private String password;
    private Integer age;

    @Builder
    private Member(Long id, String email, String password, Integer age) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.age = age;
    }

    public void update(Member member) {
        this.email = member.email;
        this.password = member.password;
        this.age = member.age;
    }

    public void checkPassword(String password) {
        if (!Objects.equals(this.password, password)) {
            throw new AuthorizationException();
        }
    }
}
