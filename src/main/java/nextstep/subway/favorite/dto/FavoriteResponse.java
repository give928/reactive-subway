package nextstep.subway.favorite.dto;

import lombok.Builder;
import lombok.Getter;
import nextstep.subway.station.dto.StationResponse;

@Getter
public class FavoriteResponse {
    private final Long id;
    private final StationResponse source;
    private final StationResponse target;

    @Builder
    private FavoriteResponse(Long id, StationResponse source, StationResponse target) {
        this.id = id;
        this.source = source;
        this.target = target;
    }
}
