package nextstep.subway.favorite.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nextstep.subway.common.entity.BaseEntity;
import org.springframework.data.annotation.Id;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Favorite extends BaseEntity {
    @Id
    private Long id;
    private Long memberId;
    private Long sourceStationId;
    private Long targetStationId;

    @Builder
    private Favorite(Long id, Long memberId, Long sourceStationId, Long targetStationId) {
        this.id = id;
        this.memberId = memberId;
        this.sourceStationId = sourceStationId;
        this.targetStationId = targetStationId;
    }

    public boolean isCreatedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
