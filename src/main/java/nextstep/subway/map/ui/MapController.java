package nextstep.subway.map.ui;

import lombok.RequiredArgsConstructor;
import nextstep.subway.map.application.MapService;
import nextstep.subway.map.dto.PathResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class MapController {
    private final MapService mapService;

    @GetMapping("/paths")
    public Mono<ResponseEntity<PathResponse>> findPath(@RequestParam Long source, @RequestParam Long target) {
        return mapService.findPath(source, target)
                .map(ResponseEntity::ok);
    }
}
