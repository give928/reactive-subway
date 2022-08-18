package nextstep.subway.line.dto;

import lombok.Builder;
import lombok.Getter;
import nextstep.subway.line.domain.Line;

import java.time.LocalDateTime;

@Getter
public class LinesResponse {
    private final Long id;
    private final String name;
    private final String color;
    private final LocalDateTime createdDate;
    private final LocalDateTime modifiedDate;

    @Builder
    private LinesResponse(Long id, String name, String color, LocalDateTime createdDate, LocalDateTime modifiedDate) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
    }

    public static LinesResponse of(Line line) {
        return LinesResponse.builder()
                .id(line.getId())
                .name(line.getName())
                .color(line.getColor())
                .createdDate(line.getCreatedDate())
                .modifiedDate(line.getModifiedDate())
                .build();
    }
}
