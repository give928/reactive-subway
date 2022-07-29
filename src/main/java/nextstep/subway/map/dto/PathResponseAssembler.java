package nextstep.subway.map.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import nextstep.subway.map.domain.SubwayPath;
import nextstep.subway.station.dto.StationResponse;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PathResponseAssembler {
    public static PathResponse assemble(SubwayPath subwayPath) {
        List<StationResponse> stationResponses = subwayPath.getStations().stream()
                .map(StationResponse::of)
                .collect(Collectors.toList());

        int distance = subwayPath.calculateDistance();

        return new PathResponse(stationResponses, distance);
    }
}
