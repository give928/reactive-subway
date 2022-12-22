package nextstep.subway.line.domain;

import nextstep.subway.common.domain.service.DomainService;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DomainService
@Transactional(readOnly = true)
public class LineDomainService {
    private final LineRepository lineRepository;
    private final SectionRepository sectionRepository;
    private final StationRepository stationRepository;

    public LineDomainService(LineRepository lineRepository, SectionRepository sectionRepository,
                             StationRepository stationRepository) {
        this.lineRepository = lineRepository;
        this.sectionRepository = sectionRepository;
        this.stationRepository = stationRepository;
    }

    // @formatter:off
    @Transactional
    public Mono<LineResponse> save(Line line) {
        return lineRepository.save(line)
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

    // @formatter:off
    public Flux<Line> findAll() {
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
                .map(section -> section.initStation(stationMap.get(section.getUpStationId()),
                                                    stationMap.get(section.getDownStationId())));
    }

    private Mono<Map<Long, Station>> extractStations() {
        return stationRepository.findAll()
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
        return stationRepository.findAllById(extractStationIds(sections))
                .collectMap(Station::getId, Function.identity());
    }

    // @formatter:off
    private Set<Long> extractStationIds(List<Section> sections) {
        return sections.stream()
                .flatMap(section -> Stream.of(section.getUpStationId(), section.getDownStationId()))
                .collect(Collectors.toSet());
    }
    // @formatter:on

    // @formatter:off
    @Transactional
    public Mono<Line> update(Long id, Line updateLine) {
        return lineRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .map(line -> line.update(updateLine))
                .flatMap(lineRepository::save);
    }
    // @formatter:on

    @Transactional
    public Mono<Void> delete(Long id) {
        return Mono.when(sectionRepository.deleteByLineId(id), lineRepository.deleteById(id));
    }

    // @formatter:off
    @Transactional
    public Mono<Void> addLineStation(Line line, Station upStation, Station downStation, int distance) {
        return Mono.just(line.addLineSection(upStation, downStation, distance))
                .flatMapMany(sectionRepository::saveAll)
                .then();
    }
    // @formatter:on

    // @formatter:off
    @Transactional
    public Mono<Void> removeLineStation(Long lineId, Long stationId) {
        return findLineById(lineId)
                .map(line -> line.removeStation(stationId))
                .flatMap(map -> Mono.when(sectionRepository.deleteAll(map.get("removeSections")),
                                          sectionRepository.saveAll(map.get("createSections"))));
    }
    // @formatter:on
}
