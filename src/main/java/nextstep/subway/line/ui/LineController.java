package nextstep.subway.line.ui;

import nextstep.subway.line.application.LineService;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.line.dto.LineSimpleResponse;
import nextstep.subway.line.dto.SectionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@RequestMapping("/lines")
@RestController
public class LineController {
    private final LineService lineService;

    public LineController(LineService lineService) {
        this.lineService = lineService;
    }

    @PostMapping
    public Mono<ResponseEntity<LineResponse>> createLine(@RequestBody LineRequest lineRequest) {
        return lineService.saveLine(lineRequest)
                .map(lineResponse -> ResponseEntity.created(URI.create("/lines/" + lineResponse.getId()))
                                .body(lineResponse));
    }

    @GetMapping
    public Mono<ResponseEntity<List<LineSimpleResponse>>> findAllLines() {
        return lineService.findLineResponses()
                .collectList()
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<LineResponse>> findLineById(@PathVariable Long id) {
        return lineService.findLineResponseById(id)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Void>> updateLine(@PathVariable Long id, @RequestBody LineRequest lineUpdateRequest) {
        return lineService.updateLine(id, lineUpdateRequest)
                .map(line -> ResponseEntity.ok().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteLine(@PathVariable Long id) {
        return lineService.deleteLineById(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @PostMapping("/{lineId}/sections")
    public Mono<ResponseEntity<Void>> addLineStation(@PathVariable Long lineId, @RequestBody SectionRequest sectionRequest) {
        return lineService.addLineStation(lineId, sectionRequest)
                .map(v -> ResponseEntity.ok().build());
    }

    @DeleteMapping("/{lineId}/sections")
    public Mono<ResponseEntity<Void>> removeLineStation(@PathVariable Long lineId, @RequestParam Long stationId) {
        return lineService.removeLineStation(lineId, stationId)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
