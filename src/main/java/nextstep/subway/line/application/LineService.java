package nextstep.subway.line.application;

import lombok.RequiredArgsConstructor;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.domain.Section;
import nextstep.subway.line.domain.SectionRepository;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.LinesResponse;
import nextstep.subway.line.dto.SectionRequest;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LineService {
    private final LineRepository lineRepository;
    private final SectionRepository sectionRepository;
    private final StationService stationService;

    // @formatter:off
    @Transactional
    public Mono<LineResponse> saveLine(LineRequest request) {
        return Mono.just(request)
                .flatMap(r -> stationService.findById(request.getUpStationId())
                        .flatMap(upStation -> Mono.just(Tuples.of(r, upStation))))
                .flatMap(tuple -> stationService.findById(request.getDownStationId())
                        .flatMap(downStation -> Mono.just(Tuples.of(tuple.getT1(), tuple.getT2(), downStation))))
                .map(tuple -> new Line(tuple.getT1().getName(), tuple.getT1().getColor(),
                                       tuple.getT2(), tuple.getT3(), tuple.getT1().getDistance()))
                .flatMap(lineRepository::save)
                .onErrorMap(RuntimeException::new)
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(savedLine -> sectionRepository.saveAll(savedLine.getSections()
                                                                        .stream()
                                                                        .map(section -> section.initLine(savedLine))
                                                                        .collect(Collectors.toList()))
                        .collectList()
                        .then(Mono.just(savedLine)))
                .map(LineResponse::of);
    }
    // @formatter:on

    public Flux<LinesResponse> findLineResponses() {
        return findAll().map(LinesResponse::of);
    }

    public Flux<Line> findLines() {
        return findAll();
    }

    // @formatter:off
    private Flux<Line> findAll() {
        return lineRepository.findAll()
                .collectList()
                .flatMap(line -> extractSections().flatMap(sections -> Mono.just(Tuples.of(line, sections))))
                .map(tuple -> tuple.getT1()
                        .stream()
                        .map(line -> line.initSections((List<Section>) tuple.getT2().get(line.getId()))))
                .flatMapMany(Flux::fromStream);
    }
    // @formatter:on

    // @formatter:off
    private Mono<Map<Long, Collection<Section>>> extractSections() {
        return sectionRepository.findAll()
                .collectList()
                .flatMap(sections -> extractStations().flatMap(stations -> Mono.just(Tuples.of(sections, stations))))
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
                .flatMap(line -> extractSections(line.getId()).flatMap(sections -> Mono.just(Tuples.of(line, sections))))
                .map(tuple -> tuple.getT1().initSections(tuple.getT2()));
    }
    // @formatter:on

    // @formatter:off
    private Mono<List<Section>> extractSections(Long id) {
        return sectionRepository.findByLineId(id)
                .collectList()
                .flatMap(sections -> extractStations(sections).flatMap(stations -> Mono.just(Tuples.of(sections, stations))))
                .map(tuple -> mapSectionStream(tuple.getT1(), tuple.getT2()).collect(Collectors.toList()));
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
    @Transactional
    public Mono<Line> updateLine(Long id, LineRequest lineUpdateRequest) {
        return lineRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .map(line -> line.update(new Line(lineUpdateRequest.getName(), lineUpdateRequest.getColor())))
                .flatMap(lineRepository::save);
    }
    // @formatter:on

    // @formatter:off
    @Transactional
    public Mono<Void> deleteLineById(Long id) {
        return Mono.just(id)
                .flatMap(lineId -> sectionRepository.deleteByLineId(lineId)
                        .thenReturn(lineId))
                .flatMap(lineId -> lineRepository.deleteById(lineId)
                        .then());
    }
    // @formatter:on

    // @formatter:off
    @Transactional
    public Mono<Void> addLineStation(Long lineId, SectionRequest request) {
        return findLineById(lineId)
                .flatMap(line -> stationService.findById(request.getUpStationId())
                        .flatMap(upStation -> Mono.just(Tuples.of(line, upStation))))
                .flatMap(tuple -> stationService.findById(request.getDownStationId())
                        .flatMap(downStation -> Mono.just(Tuples.of(tuple.getT1(), tuple.getT2(), downStation))))
                .map(tuple -> tuple.getT1().addLineSection(tuple.getT2(), tuple.getT3(), request.getDistance()))
                .flatMapMany(sectionRepository::saveAll)
                .then();
    }
    // @formatter:on

    // @formatter:off
    @Transactional
    public Mono<Void> removeLineStation(Long lineId, Long stationId) {
        return findLineById(lineId)
                .map(line -> line.removeStation(stationId))
                .flatMap(map -> {
                    Mono<Void> removeSections = sectionRepository.deleteAll(map.get("removeSections"));
                    Mono<Void> createSections = sectionRepository.saveAll(map.get("createSections")).then();
                    return Mono.when(removeSections, createSections);
                });
    }
    // @formatter:on
}
