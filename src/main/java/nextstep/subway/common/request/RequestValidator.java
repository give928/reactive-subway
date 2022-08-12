package nextstep.subway.common.request;

import lombok.RequiredArgsConstructor;
import nextstep.subway.station.dto.StationRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.server.ServerWebInputException;

@Component
@RequiredArgsConstructor
public class RequestValidator {
    private final Validator validator;

    public void validate(StationRequest stationRequest) {
        Errors errors = new BeanPropertyBindingResult(stationRequest, StationRequest.class.getName());
        validator.validate(stationRequest, errors);
        if (errors.hasErrors()) {
            throw new ServerWebInputException(errors.toString());
        }
    }
}
