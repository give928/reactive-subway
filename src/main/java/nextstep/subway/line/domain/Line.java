package nextstep.subway.line.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.station.domain.Station;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@NoArgsConstructor
@Getter
public class Line implements Serializable {
    private static final long serialVersionUID = -1249015528403540198L;

    @Id
    private Long id;
    private String name;
    private String color;
    @CreatedDate
    private LocalDateTime createdDate;
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    @JsonManagedReference
    @Transient
    private List<Section> sections = new ArrayList<>();

    public Line(String name, String color) {
        this.name = name;
        this.color = color;
    }

    // @formatter:off
    public Line(String name, String color, Station upStation, Station downStation, int distance) {
        this.name = name;
        this.color = color;
        addSection(Section.builder()
                           .line(this)
                           .upStation(upStation)
                           .downStation(downStation)
                           .distance(distance)
                           .build());
    }
    // @formatter:on

    Line initSections(List<Section> sections) {
        this.sections = sections;
        return this;
    }

    private void addSection(Section section) {
        sections.add(section);
        section.initLine(this);
    }

    public Line update(Line line) {
        this.name = line.getName();
        this.color = line.getColor();
        return this;
    }

    // @formatter:off
    List<Section> addLineSection(Station upStation, Station downStation, int distance) {
        boolean isUpStationExisted = isExisted(upStation);
        boolean isDownStationExisted = isExisted(downStation);
        valid(isUpStationExisted, isDownStationExisted);

        List<Section> saveSections = new ArrayList<>();
        if (isUpStationExisted) {
            updateUpStation(upStation, downStation, distance).ifPresent(saveSections::add);
        }
        if (isDownStationExisted) {
            updateDownStation(upStation, downStation, distance).ifPresent(saveSections::add);
        }
        saveSections.add(Section.builder()
                                 .line(this)
                                 .upStation(upStation)
                                 .downStation(downStation)
                                 .distance(distance)
                                 .build());
        return saveSections;
    }
    // @formatter:on

    Map<String, List<Section>> removeStation(Long stationId) {
        validateRemoveStation();

        Optional<Section> upLineStation = findUpStation(stationId);
        Optional<Section> downLineStation = findDownStation(stationId);

        List<Section> createSections = new ArrayList<>();
        if (upLineStation.isPresent() && downLineStation.isPresent()) {
            createSections.add(createSection(upLineStation.get(), downLineStation.get()));
        }

        List<Section> removeSections = new ArrayList<>();
        upLineStation.ifPresent(section -> {
            removeSections.add(section);
            sections.remove(section);
        });
        downLineStation.ifPresent(section -> {
            removeSections.add(section);
            sections.remove(section);
        });
        return Map.of("createSections", createSections, "removeSections", removeSections);
    }

    private void validateRemoveStation() {
        if (sections.size() <= 1) {
            throw new IllegalArgumentException("라인에서 역을 제거할 수 없습니다.");
        }
    }

    public List<Station> getStations() {
        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        return orderBySection();
    }

    private List<Station> orderBySection() {
        List<Station> stations = new ArrayList<>();
        Station station = findFirstStation();
        stations.add(station);

        while (isPresentNextSection(station)) {
            Section nextSection = findNextSection(station);
            station = nextSection.getDownStation();
            stations.add(station);
        }
        return stations;
    }

    private Station findFirstStation() {
        Station upStation = sections.get(0).getUpStation();
        while (isPresentPreSection(upStation)) {
            Section nextSection = findPreSection(upStation);
            upStation = nextSection.getUpStation();
        }
        return upStation;
    }

    // @formatter:off
    private boolean isPresentPreSection(Station station) {
        return sections.stream()
                .filter(Section::existDownStation)
                .anyMatch(it -> it.equalDownStation(station));
    }
    // @formatter:on

    // @formatter:off
    private boolean isPresentNextSection(Station station) {
        return sections.stream()
                .filter(Section::existUpStation)
                .anyMatch(it -> it.equalUpStation(station));
    }
    // @formatter:on

    // @formatter:off
    private Section findPreSection(Station station) {
        return sections.stream()
                .filter(it -> it.equalDownStation(station))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("이전 구간이 없습니다."));
    }
    // @formatter:on

    // @formatter:off
    private Section findNextSection(Station station) {
        return sections.stream()
                .filter(it -> it.equalUpStation(station))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("다음 구간이 없습니다."));
    }
    // @formatter:on

    private void valid(boolean isUpStationExisted, boolean isDownStationExisted) {
        if (isUpStationExisted && isDownStationExisted) {
            throw new IllegalArgumentException("이미 등록된 구간 입니다.");
        }

        if (!getSections().isEmpty() && !isUpStationExisted && !isDownStationExisted) {
            throw new IllegalArgumentException("등록할 수 없는 구간 입니다.");
        }
    }

    // @formatter:off
    private Optional<Section> updateDownStation(Station upStation, Station downStation, int distance) {
        Optional<Section> section = sections.stream()
                .filter(it -> Objects.equals(it.getDownStationId(), downStation.getId()))
                .findFirst();
        section.ifPresent(it -> it.updateDownStation(upStation, distance));
        return section;
    }
    // @formatter:on

    // @formatter:off
    private Optional<Section> updateUpStation(Station upStation, Station downStation, int distance) {
        Optional<Section> section = sections.stream()
                .filter(it -> Objects.equals(it.getUpStationId(), upStation.getId()))
                .findFirst();
        section.ifPresent(it -> it.updateUpStation(downStation, distance));
        return section;
    }
    // @formatter:on

    private boolean isExisted(Station station) {
        return getStations().stream()
                .anyMatch(s -> s.getId().equals(station.getId()));
    }

    // @formatter:off
    private Section createSection(Section upLineStation, Section downLineStation) {
        int newDistance = upLineStation.getDistance() + downLineStation.getDistance();
        Section section = Section.builder()
                .line(this)
                .upStation(downLineStation.getUpStation())
                .downStation(upLineStation.getDownStation())
                .distance(newDistance)
                .build();
        addSection(section);
        return section;
    }
    // @formatter:on

    private Optional<Section> findDownStation(Long stationId) {
        return sections.stream()
                .filter(it -> it.equalDownStation(stationId))
                .findFirst();
    }

    private Optional<Section> findUpStation(Long stationId) {
        return sections.stream()
                .filter(it -> it.equalUpStation(stationId))
                .findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Line line = (Line) o;
        return Objects.equals(getId(), line.getId()) && Objects.equals(getName(),
                                                                       line.getName()) && Objects.equals(
                getColor(), line.getColor()) && Objects.equals(getCreatedDate(),
                                                               line.getCreatedDate()) && Objects.equals(
                getModifiedDate(), line.getModifiedDate()) && Objects.equals(getSections(), line.getSections());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getColor(), getCreatedDate(), getModifiedDate(), getSections());
    }
}
