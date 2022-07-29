package nextstep.subway.map.dto;

import lombok.*;
import nextstep.subway.station.dto.StationResponse;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
public class PathResponse {
    private List<StationResponse> stations;
    private int distance;
}
