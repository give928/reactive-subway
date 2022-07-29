package nextstep.subway.station.application;

import lombok.RequiredArgsConstructor;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationRepository;
import nextstep.subway.station.dto.StationRequest;
import nextstep.subway.station.dto.StationResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StationService {
    private final StationRepository stationRepository;

    @CacheEvict(value = {"stations", "lines", "line-responses", "line", "line-response", "find-path"}, allEntries = true)
    @Transactional
    public StationResponse saveStation(StationRequest stationRequest) {
        Station persistStation = stationRepository.save(stationRequest.toStation());
        return StationResponse.of(persistStation);
    }

    @Cacheable("stations")
    public List<StationResponse> findAllStations() {
        List<Station> stations = stationRepository.findAll();

        return stations.stream()
                .map(StationResponse::of)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "stations", key = "{#id, #pageable.pageSize}", unless = "#result.isEmpty()")
    public List<StationResponse> findStations(Long id, Pageable pageable) {
        return stationRepository.findStations(id, pageable)
                .stream()
                .map(StationResponse::of)
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"stations", "lines", "line-responses", "line", "line-response", "find-path"}, allEntries = true)
    @Transactional
    public void deleteStationById(Long id) {
        stationRepository.deleteById(id);
    }

    public Station findStationById(Long id) {
        return stationRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public Station findById(Long id) {
        return stationRepository.findById(id).orElseThrow(RuntimeException::new);
    }
}
