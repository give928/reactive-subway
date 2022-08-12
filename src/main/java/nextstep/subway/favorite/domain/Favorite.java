package nextstep.subway.favorite.domain;

import lombok.*;
import nextstep.subway.common.entity.BaseEntity;
import org.springframework.data.annotation.Id;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class Favorite extends BaseEntity {
    @Id
    private Long id;
    private Long memberId;
    private Long sourceStationId;
    private Long targetStationId;

    public boolean isCreatedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
