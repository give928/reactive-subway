package nextstep.subway.favorite.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nextstep.subway.station.dto.StationResponse;

@RequiredArgsConstructor
@Builder
@Getter
public class FavoriteResponse {
    private final Long id;
    private final StationResponse source;
    private final StationResponse target;
}
