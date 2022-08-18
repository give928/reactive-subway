package nextstep.subway.map.domain;

import lombok.Getter;
import nextstep.subway.line.domain.Section;
import org.jgrapht.graph.DefaultWeightedEdge;

@Getter
public class SectionEdge extends DefaultWeightedEdge {
    private static final long serialVersionUID = -3219619267282508226L;

    private final Section section;
    private final Long lineId;

    public SectionEdge(Section section, Long lineId) {
        this.section = section;
        this.lineId = lineId;
    }

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
