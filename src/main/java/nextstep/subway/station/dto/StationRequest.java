package nextstep.subway.station.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.station.domain.Station;

import javax.validation.constraints.NotBlank;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class StationRequest {
    @NotBlank(message = "{validation.station.name}")
    private String name;

    public Station toStation() {
        return new Station(name);
    }
}
