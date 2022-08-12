package nextstep.subway.station.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.common.entity.BaseEntity;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@Getter
public class Station extends BaseEntity implements Serializable {
    private static final long serialVersionUID = -3592742922755525695L;

    @Id
    private Long id;
    @Column
    private String name;

    public Station(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Station station = (Station) o;
        return Objects.equals(id, station.id) &&
                Objects.equals(name, station.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
