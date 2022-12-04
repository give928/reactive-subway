package study.unit;

import nextstep.subway.line.application.LineService;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.domain.Section;
import nextstep.subway.line.domain.SectionRepository;
import nextstep.subway.line.dto.LineSimpleResponse;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("단위 테스트 - mockito를 활용한 가짜 협력 객체 사용")
class MockitoTest {
    @Test
    void findAllLines() {
        // given
        LineRepository lineRepository = mock(LineRepository.class);
        SectionRepository sectionRepository = mock(SectionRepository.class);
        StationService stationService = mock(StationService.class);

        when(lineRepository.findAll()).thenReturn(Flux.just(new Line()));
        when(sectionRepository.findAll()).thenReturn(Flux.just(new Section()));
        when(stationService.findAll()).thenReturn(Flux.just(new Station()));
        LineService lineService = new LineService(lineRepository, sectionRepository, stationService);

        // when
        Flux<LineSimpleResponse> responses = lineService.findLineResponses();

        // then
        StepVerifier.create(responses)
                .expectNextCount(1)
                .verifyComplete();
    }
}
