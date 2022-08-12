package nextstep.subway.common.config.auth;

import lombok.RequiredArgsConstructor;
import nextstep.subway.auth.application.AuthService;
import nextstep.subway.auth.ui.AuthenticationPrincipalArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;

@Configuration
@RequiredArgsConstructor
public class AuthenticationPrincipalConfig implements WebFluxConfigurer {
    private final AuthService authService;

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new AuthenticationPrincipalArgumentResolver(authService));
    }
}
