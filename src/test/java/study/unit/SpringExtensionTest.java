package study.unit;

import nextstep.subway.line.application.LineService;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.LineRepository;
import nextstep.subway.line.domain.Section;
import nextstep.subway.line.domain.SectionRepository;
import nextstep.subway.line.dto.LinesResponse;
import nextstep.subway.station.application.StationService;
import nextstep.subway.station.domain.Station;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@DisplayName("단위 테스트 - SpringExtension을 활용한 가짜 협력 객체 사용")
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
class SpringExtensionTest {
    @MockBean
    private LineRepository lineRepository;
    @MockBean
    private SectionRepository sectionRepository;
    @MockBean
    private StationService stationService;

    @Test
    void findAllLines() {
        // given
        when(lineRepository.findAll()).thenReturn(Flux.just(new Line()));
        when(sectionRepository.findAll()).thenReturn(Flux.just(new Section()));
        when(stationService.findAll()).thenReturn(Flux.just(new Station()));
        LineService lineService = new LineService(lineRepository, sectionRepository, stationService);

        // when
        Flux<LinesResponse> responses = lineService.findLineResponses();

        // then
        StepVerifier.create(responses)
                .expectNextCount(1)
                .verifyComplete();
    }
}
