package nextstep.subway.favorite.application;

import nextstep.subway.auth.domain.LoginMember;
import nextstep.subway.favorite.domain.Favorite;
import nextstep.subway.favorite.domain.FavoriteDomainService;
import nextstep.subway.favorite.dto.FavoriteRequest;
import nextstep.subway.favorite.dto.FavoriteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
public class FavoriteService {
    private final FavoriteDomainService favoriteDomainService;

    public FavoriteService(FavoriteDomainService favoriteDomainService) {
        this.favoriteDomainService = favoriteDomainService;
    }

    // @formatter:off
    @Transactional
    public Mono<Favorite> createFavorite(LoginMember loginMember, FavoriteRequest request) {
        return favoriteDomainService.save(Favorite.builder()
                                               .memberId(loginMember.getId())
                                               .sourceStationId(request.getSource())
                                               .targetStationId(request.getTarget())
                                               .build());
    }
    // @formatter:on

    public Flux<FavoriteResponse> findFavorites(LoginMember loginMember) {
        return favoriteDomainService.findByMemberId(loginMember.getId());
    }

    @Transactional
    public Mono<Void> deleteFavorite(LoginMember loginMember, Long id) {
        return favoriteDomainService.delete(loginMember.getId(), id);
    }
}
