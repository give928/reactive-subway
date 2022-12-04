package nextstep.subway.map.application;

import nextstep.subway.common.log.annotation.Loggable;
import nextstep.subway.line.application.LineService;
import nextstep.subway.map.dto.PathResponse;
import nextstep.subway.map.dto.PathResponseAssembler;
import nextstep.subway.station.application.StationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
public class MapService {
    private final LineService lineService;
    private final StationService stationService;
    private final PathService pathService;

    public MapService(LineService lineService, StationService stationService, PathService pathService) {
        this.lineService = lineService;
        this.stationService = stationService;
        this.pathService = pathService;
    }

    // @formatter:off
    @Loggable(json = true)
    public Mono<PathResponse> findPath(Long source, Long target) {
        return Mono.zip(lineService.findLines()
                                .collectList(),
                        stationService.findById(source),
                        stationService.findById(target))
                .flatMap(tuple -> pathService.findPath(tuple.getT1(), tuple.getT2(), tuple.getT3()))
                .map(PathResponseAssembler::assemble);
    }
    // @formatter:on
}
