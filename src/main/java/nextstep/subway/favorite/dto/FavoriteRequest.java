package nextstep.subway.favorite.dto;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class FavoriteRequest {
    private Long source;
    private Long target;
}
