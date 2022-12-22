package nextstep.subway.favorite.domain;

import nextstep.subway.common.domain.service.DomainService;
import nextstep.subway.favorite.dto.FavoriteResponse;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import nextstep.subway.station.dto.StationResponse;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DomainService
@Transactional(readOnly = true)
public class FavoriteDomainService {
    private final FavoriteRepository favoriteRepository;
    private final StationRepository stationRepository;

    public FavoriteDomainService(FavoriteRepository favoriteRepository, StationRepository stationRepository) {
        this.favoriteRepository = favoriteRepository;
        this.stationRepository = stationRepository;
    }

    @Transactional
    public Mono<Favorite> save(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    // @formatter:off
    public Flux<FavoriteResponse> findByMemberId(Long memberId) {
        return favoriteRepository.findByMemberId(memberId)
                .collectList()
                .zipWhen(this::extractStations)
                .map(tuple -> mapFavoriteStream(tuple.getT1(), tuple.getT2()))
                .flatMapMany(Flux::fromStream);
    }
    // @formatter:on

    private Mono<Map<Long, Station>> extractStations(List<Favorite> favorites) {
        return stationRepository.findAllById(extractStationIds(favorites))
                .collectMap(Station::getId, Function.identity());
    }

    // @formatter:off
    private Set<Long> extractStationIds(List<Favorite> favorites) {
        return favorites.stream()
                .flatMap(favorite -> Stream.of(favorite.getSourceStationId(), favorite.getTargetStationId()))
                .collect(Collectors.toSet());
    }
    // @formatter:on

    // @formatter:off
    private static Stream<FavoriteResponse> mapFavoriteStream(List<Favorite> favorites, Map<Long, Station> stations) {
        return favorites.stream()
                .map(favorite -> FavoriteResponse.builder()
                        .id(favorite.getId())
                        .source(StationResponse.of(stations.get(favorite.getSourceStationId())))
                        .target(StationResponse.of(stations.get(favorite.getTargetStationId())))
                        .build());
    }
    // @formatter:on

    // @formatter:off
    @Transactional
    public Mono<Void> delete(Long memberId, Long id) {
        return favoriteRepository.findById(id)
                .doOnNext(favorite -> {
                    if (!favorite.isCreatedBy(memberId)) {
                        throw new HasNotPermissionException(String.format("%s는 삭제할 권한이 없습니다.", memberId));
                    }
                })
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .flatMap(favoriteRepository::delete);
    }
    // @formatter:on
}
