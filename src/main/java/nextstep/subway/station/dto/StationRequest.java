package nextstep.subway.station.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.station.domain.Station;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class StationRequest {
    private String name;

    public Station toStation() {
        return new Station(name);
    }
}
