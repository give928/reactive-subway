package nextstep.subway.member.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.member.domain.Member;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MemberRequest {
    private String email;
    private String password;
    private Integer age;

    public Member toMember() {
        return Member.builder()
                .email(email)
                .password(password)
                .age(age)
                .build();
    }
}
