package nextstep.subway.auth.dto;

import lombok.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class TokenResponse {
    private String accessToken;
}
