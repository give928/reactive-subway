package nextstep.subway.line.application;

import nextstep.subway.common.cache.annotation.ReactiveCacheEvict;
import nextstep.subway.common.cache.annotation.ReactiveCacheable;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineDomainService;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.LineSimpleResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
public class LineService {
    private final LineDomainService lineDomainService;
    private final StationService stationService;

    public LineService(LineDomainService lineDomainService, StationService stationService) {
        this.lineDomainService = lineDomainService;
        this.stationService = stationService;
    }

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<LineResponse> saveLine(LineRequest request) {
        return Mono.zip(stationService.findById(request.getUpStationId()),
                        stationService.findById(request.getDownStationId()))
                .map(tuple -> new Line(request.getName(), request.getColor(), tuple.getT1(), tuple.getT2(), request.getDistance()))
                .flatMap(lineDomainService::save);
    }
    // @formatter:on

    @ReactiveCacheable("line-simple-responses")
    public Flux<LineSimpleResponse> findLineResponses() {
        return lineDomainService.findAll()
                .map(LineSimpleResponse::of);
    }

    @ReactiveCacheable("lines")
    public Flux<Line> findLines() {
        return lineDomainService.findAll();
    }

    public Mono<LineResponse> findLineResponseById(Long id) {
        return lineDomainService.findLineById(id)
                .map(LineResponse::of);
    }

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Line> updateLine(Long id, LineRequest lineUpdateRequest) {
        return lineDomainService.update(id, new Line(lineUpdateRequest.getName(), lineUpdateRequest.getColor()));
    }
    // @formatter:on

    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Void> deleteLineById(Long id) {
        return lineDomainService.delete(id);
    }

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Void> addLineStation(Long lineId, SectionRequest request) {
        return Mono.zip(lineDomainService.findLineById(lineId),
                        stationService.findById(request.getUpStationId()),
                        stationService.findById(request.getDownStationId()))
                .flatMap(tuple -> lineDomainService.addLineStation(tuple.getT1(), tuple.getT2(), tuple.getT3(),
                                                                   request.getDistance()));
    }
    // @formatter:on

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Void> removeLineStation(Long lineId, Long stationId) {
        return lineDomainService.removeLineStation(lineId, stationId);
    }
    // @formatter:on
}
