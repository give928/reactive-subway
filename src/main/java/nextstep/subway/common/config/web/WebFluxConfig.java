package nextstep.subway.common.config.web;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {
    private static final String STATIC_RESOURCE_PATTERN = "/**";
    static final String STATIC_JS_RESOURCE_PATTERN = "/js/**";
    static final String STATIC_CSS_RESOURCE_PATTERN = "/css/**";
    @Value("${spring.web.resources.static-locations}")
    private String staticLocations;

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

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper()));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper()));

        configurer.defaultCodecs().enableLoggingRequestDetails(true);
    }

    // @formatter:off
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .failOnUnknownProperties(false)
                .featuresToDisable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .serializerByType(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_DATE))
                .build();
    }
    // @formatter:off

    @Bean
    public DefaultResourceLoader defaultResourceLoader() {
        return new DefaultResourceLoader();
    }
}
