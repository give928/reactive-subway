package nextstep.subway.map.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nextstep.subway.line.domain.Section;
import org.jgrapht.graph.DefaultWeightedEdge;

@RequiredArgsConstructor
@Getter
public class SectionEdge extends DefaultWeightedEdge {
    private static final long serialVersionUID = -3219619267282508226L;

    private final Section section;
    private final Long lineId;

    @Override
    protected Object getSource() {
        return this.section.getUpStation();
    }

    @Override
    protected Object getTarget() {
        return this.section.getDownStation();
    }

    @Override
    protected double getWeight() {
        return this.section.getDistance();
    }
}
