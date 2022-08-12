package nextstep.subway.favorite.ui;

import lombok.RequiredArgsConstructor;
import nextstep.subway.auth.domain.AuthenticationPrincipal;
import nextstep.subway.auth.domain.LoginMember;
import nextstep.subway.favorite.application.FavoriteService;
import nextstep.subway.favorite.dto.FavoriteRequest;
import nextstep.subway.favorite.dto.FavoriteResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@RequestMapping("/favorites")
@RestController
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping
    public Mono<ResponseEntity<Void>> createFavorite(@AuthenticationPrincipal LoginMember loginMember, @RequestBody FavoriteRequest request) {
        return favoriteService.createFavorite(loginMember, request)
                .map(favorite -> ResponseEntity.created(URI.create("/favorites/" + 1L))
                                .build());
    }

    @GetMapping
    public Mono<ResponseEntity<List<FavoriteResponse>>> getFavorites(@AuthenticationPrincipal LoginMember loginMember) {
        return favoriteService.findFavorites(loginMember)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteFavorite(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long id) {
        return favoriteService.deleteFavorite(loginMember, id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
