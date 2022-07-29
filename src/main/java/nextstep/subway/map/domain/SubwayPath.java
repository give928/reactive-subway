package nextstep.subway.map.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nextstep.subway.station.domain.Station;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class SubwayPath {
    private final List<SectionEdge> sectionEdges;
    private final List<Station> stations;

    public int calculateDistance() {
        return sectionEdges.stream().mapToInt(it -> it.getSection().getDistance()).sum();
    }
}
