package nextstep.subway.line.application;

import nextstep.subway.common.cache.annotation.ReactiveCacheEvict;
import nextstep.subway.common.cache.annotation.ReactiveCacheable;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.domain.Section;
import nextstep.subway.line.domain.SectionRepository;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.LineSimpleResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class LineService {
    private final LineRepository lineRepository;
    private final SectionRepository sectionRepository;
    private final StationService stationService;

    public LineService(LineRepository lineRepository, SectionRepository sectionRepository, StationService stationService) {
        this.lineRepository = lineRepository;
        this.sectionRepository = sectionRepository;
        this.stationService = stationService;
    }

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<LineResponse> saveLine(LineRequest request) {
        return Mono.zip(stationService.findById(request.getUpStationId()),
                        stationService.findById(request.getDownStationId()))
                .map(tuple -> mapLine(request, tuple.getT1(), tuple.getT2()))
                .flatMap(lineRepository::save)
                .onErrorMap(RuntimeException::new)
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(savedLine -> sectionRepository.saveAll(savedLine.getSections()
                                                                        .stream()
                                                                        .map(section -> section.initLine(savedLine))
                                                                        .collect(Collectors.toList()))
                        .then(Mono.just(LineResponse.of(savedLine))))
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())));
    }
    // @formatter:on

    private static Line mapLine(LineRequest request, Station upStation, Station downStation) {
        return new Line(request.getName(), request.getColor(), upStation, downStation, request.getDistance());
    }

    @ReactiveCacheable("line-simple-responses")
    public Flux<LineSimpleResponse> findLineResponses() {
        return findAll().map(LineSimpleResponse::of);
    }

    @ReactiveCacheable("lines")
    public Flux<Line> findLines() {
        return findAll();
    }

    // @formatter:off
    private Flux<Line> findAll() {
        return lineRepository.findAll()
                .collectList()
                .zipWith(extractSections())
//                .flatMap(lines -> extractSections().flatMap(sections -> Mono.just(Tuples.of(lines, sections))))
                .map(tuple -> tuple.getT1()
                        .stream()
                        .map(line -> line.initSections(new ArrayList<>(tuple.getT2()
                                                               .get(line.getId())))))
                .flatMapMany(Flux::fromStream);

    }
    // @formatter:on

    // @formatter:off
    private Mono<Map<Long, Collection<Section>>> extractSections() {
        return sectionRepository.findAll()
                .collectList()
                .zipWith(extractStations())
//                .flatMap(sections -> extractStations().flatMap(stations -> Mono.just(Tuples.of(sections, stations))))
                .map(tuple -> mapSectionStream(tuple.getT1(), tuple.getT2()))
                .flatMapMany(Flux::fromStream)
                .collectMultimap(Section::getLineId);
    }
    // @formatter:on

    private static Stream<Section> mapSectionStream(List<Section> sections, Map<Long, Station> stationMap) {
        return sections.stream()
                .map(section -> section.initUpStation(stationMap.get(section.getUpStationId()))
                        .initDownStation(stationMap.get(section.getDownStationId())));
    }

    private Mono<Map<Long, Station>> extractStations() {
        return stationService.findAll()
                .collectMap(Station::getId, Function.identity());
    }

    // @formatter:off
    public Mono<Line> findLineById(Long id) {
        return lineRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .zipWhen(line -> extractSections(line.getId()))
                .map(tuple -> tuple.getT1()
                        .initSections(tuple.getT2()));
    }
    // @formatter:on

    // @formatter:off
    private Mono<List<Section>> extractSections(Long id) {
        return sectionRepository.findByLineId(id)
                .collectList()
                .zipWhen(this::extractStations)
                .map(tuple -> mapSectionStream(tuple.getT1(), tuple.getT2())
                        .collect(Collectors.toList()));
    }
    // @formatter:on

    private Mono<Map<Long, Station>> extractStations(List<Section> sections) {
        return stationService.findAllById(extractStationIds(sections))
                .collectMap(Station::getId, Function.identity());
    }

    // @formatter:off
    private Set<Long> extractStationIds(List<Section> sections) {
        return sections.stream()
                .flatMap(section -> Stream.of(section.getUpStationId(), section.getDownStationId()))
                .collect(Collectors.toSet());
    }
    // @formatter:on

    public Mono<LineResponse> findLineResponseById(Long id) {
        return findLineById(id).map(LineResponse::of);
    }

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Line> updateLine(Long id, LineRequest lineUpdateRequest) {
        return lineRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .doOnNext(line -> line.update(new Line(lineUpdateRequest.getName(), lineUpdateRequest.getColor())))
                .flatMap(lineRepository::save);
    }
    // @formatter:on

    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Void> deleteLineById(Long id) {
        return Mono.when(sectionRepository.deleteByLineId(id), lineRepository.deleteById(id));
    }

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Void> addLineStation(Long lineId, SectionRequest request) {
        return findLineById(lineId)
                .flatMap(line -> Mono.zip(stationService.findById(request.getUpStationId()),
                                          stationService.findById(request.getDownStationId()))
                        .map(tuple -> line.addLineSection(tuple.getT1(), tuple.getT2(), request.getDistance())))
                .flatMapMany(sectionRepository::saveAll)
                .then();
    }
    // @formatter:on

    // @formatter:off
    @ReactiveCacheEvict(value = {"lines", "line-simple-responses"})
    @Transactional
    public Mono<Void> removeLineStation(Long lineId, Long stationId) {
        return findLineById(lineId)
                .map(line -> line.removeStation(stationId))
                .flatMap(map -> Mono.when(sectionRepository.deleteAll(map.get("removeSections")),
                                          sectionRepository.saveAll(map.get("createSections"))));
    }
    // @formatter:on
}
