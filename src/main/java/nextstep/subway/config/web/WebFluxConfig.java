package nextstep.subway.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    private static final String STATIC_RESOURCE_PATTERN = "/**";
    static final String STATIC_JS_RESOURCE_PATTERN = "/js/**";
    static final String STATIC_CSS_RESOURCE_PATTERN = "/css/**";
    @Value("${spring.web.resources.static-locations}")
    private String staticLocations;

    private final ObjectMapper objectMapper;

    public WebFluxConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // @formatter:off
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(STATIC_RESOURCE_PATTERN)
                .addResourceLocations(staticLocations)
                .setCacheControl(CacheControl.noCache().cachePrivate());

        registry.addResourceHandler(STATIC_JS_RESOURCE_PATTERN)
                .addResourceLocations(staticLocations + "js/")
                .setCacheControl(CacheControl.noCache().cachePrivate());

        registry.addResourceHandler(STATIC_CSS_RESOURCE_PATTERN)
                .addResourceLocations(staticLocations + "css/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS));
    }
    // @formatter:on

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));

        configurer.defaultCodecs().enableLoggingRequestDetails(true);
    }

    @Bean
    public DefaultResourceLoader defaultResourceLoader() {
        return new DefaultResourceLoader();
    }
}
