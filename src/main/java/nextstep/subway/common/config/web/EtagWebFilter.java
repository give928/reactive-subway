package nextstep.subway.common.config.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.*;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static nextstep.subway.common.config.web.WebFluxConfig.STATIC_CSS_RESOURCE_PATTERN;
import static nextstep.subway.common.config.web.WebFluxConfig.STATIC_JS_RESOURCE_PATTERN;

@Component
@Slf4j
public class EtagWebFilter implements WebFilter {
    private static final String STATIC_ROOT_RESOURCE_PATTERN = "/?*";
    private static final List<String> pathPatterns = Arrays.asList(STATIC_ROOT_RESOURCE_PATTERN,
                                                                   STATIC_JS_RESOURCE_PATTERN,
                                                                   STATIC_CSS_RESOURCE_PATTERN);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final DefaultResourceLoader defaultResourceLoader;

    @Value("${spring.web.resources.static-locations}")
    private String staticLocations;

    public EtagWebFilter(DefaultResourceLoader defaultResourceLoader) {
        this.defaultResourceLoader = defaultResourceLoader;
    }

    // @formatter:off
    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, WebFilterChain webFilterChain) {
        if (isResource(serverWebExchange)) {
            return getResource(serverWebExchange)
                    .flatMap(resource -> getEtag(resource)
                            .flatMap(etag -> {
                                log.debug("resource: {}, etag: {}", resource, etag);
                                String formattedEtag = String.format("\"%s\"", etag);
                                serverWebExchange.getResponse()
                                        .getHeaders()
                                        .setETag(formattedEtag);
                                if (isMatchEtag(serverWebExchange, formattedEtag)) {
                                    log.debug("{} response 302 not modified", resource);
                                    serverWebExchange.getResponse()
                                            .setStatusCode(HttpStatus.NOT_MODIFIED);
                                }
                                return Mono.empty();
                            }))
                    .onErrorReturn(webFilterChain.filter(serverWebExchange))
                    .thenEmpty(webFilterChain.filter(serverWebExchange));
        }
        return webFilterChain.filter(serverWebExchange);
    }
    // @formatter:on

    private boolean isResource(ServerWebExchange serverWebExchange) {
        String path = getPath(serverWebExchange);
        return pathPatterns.stream()
                .anyMatch(pathPattern -> pathMatcher.match(pathPattern, path));
    }

    private static String getPath(ServerWebExchange serverWebExchange) {
        return serverWebExchange.getRequest().getPath().toString();
    }

    private Mono<Resource> getResource(ServerWebExchange serverWebExchange) {
        String location = getPath(serverWebExchange);
        String path = ResourceHelper.processPath(location);
        if (!StringUtils.hasText(path) || ResourceHelper.isInvalidPath(path)) {
            return Mono.empty();
        }
        if (ResourceHelper.isInvalidEncodedPath(path)) {
            return Mono.empty();
        }

        return Mono.just(defaultResourceLoader.getResource(staticLocations + path.substring(1)));
    }

    // @formatter:off
    private boolean isMatchEtag(ServerWebExchange exchange, String etag) {
        return Optional.ofNullable(exchange.getRequest()
                                           .getHeaders()
                                           .get("If-None-Match"))
                .orElse(Collections.emptyList())
                .stream()
                .anyMatch(s -> Objects.equals(s, etag));
    }
    // @formatter:on

    private Mono<String> getEtag(Resource resource) {
        Flux<DataBuffer> flux = DataBufferUtils.read(resource, DefaultDataBufferFactory.sharedInstance, StreamUtils.BUFFER_SIZE);

        return DataBufferUtils.join(flux)
                .map(buffer -> {
                    byte[] result = new byte[buffer.readableByteCount()];
                    buffer.read(result);
                    DataBufferUtils.release(buffer);
                    return DigestUtils.md5DigestAsHex(result);
                });
    }

    // from org.springframework.web.reactive.resource.ResourceWebHandler
    private static class ResourceHelper {
        private static String processPath(String path) {
            path = StringUtils.replace(path, "\\", "/");
            path = cleanDuplicateSlashes(path);
            return cleanLeadingSlash(path);
        }

        private static String cleanDuplicateSlashes(String path) {
            StringBuilder sb = null;
            char prev = 0;
            for (int i = 0; i < path.length(); i++) {
                char curr = path.charAt(i);
                try {
                    if (curr == '/' && prev == '/') {
                        if (sb == null) {
                            sb = new StringBuilder(path.substring(0, i));
                        }
                        continue;
                    }
                    if (sb != null) {
                        sb.append(path.charAt(i));
                    }
                } finally {
                    prev = curr;
                }
            }
            return (sb != null ? sb.toString() : path);
        }

        private static String cleanLeadingSlash(String path) {
            boolean slash = false;
            for (int i = 0; i < path.length(); i++) {
                if (path.charAt(i) == '/') {
                    slash = true;
                } else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
                    if (i == 0 || (i == 1 && slash)) {
                        return path;
                    }
                    return (slash ? "/" + path.substring(i) : path.substring(i));
                }
            }
            return (slash ? "/" : "");
        }

        private static boolean isInvalidEncodedPath(String path) {
            if (path.contains("%")) {
                try {
                    // Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
                    String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
                    if (isInvalidPath(decodedPath)) {
                        return true;
                    }
                    decodedPath = processPath(decodedPath);
                    if (isInvalidPath(decodedPath)) {
                        return true;
                    }
                } catch (IllegalArgumentException ex) {
                    // May not be possible to decode...
                }
            }
            return false;
        }

        private static boolean isInvalidPath(String path) {
            if (isInvalidPathInf(path)) {
                return true;
            }
            if (isInvalidPathUrl(path)) {
                return true;
            }
            return isInvalidPathBack(path);
        }

        private static boolean isInvalidPathInf(String path) {
            if (path.contains("WEB-INF") || path.contains("META-INF")) {
                if (log.isWarnEnabled()) {
                    log.warn(LogFormatUtils.formatValue(
                            "Path with \"WEB-INF\" or \"META-INF\": [" + path + "]", -1, true));
                }
                return true;
            }
            return false;
        }

        private static boolean isInvalidPathUrl(String path) {
            if (path.contains(":/")) {
                String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
                if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
                    if (log.isWarnEnabled()) {
                        log.warn(LogFormatUtils.formatValue(
                                "Path represents URL or has \"url:\" prefix: [" + path + "]", -1, true));
                    }
                    return true;
                }
            }
            return false;
        }

        private static boolean isInvalidPathBack(String path) {
            if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
                if (log.isWarnEnabled()) {
                    log.warn(LogFormatUtils.formatValue(
                            "Path contains \"../\" after call to StringUtils#cleanPath: [" + path + "]", -1, true));
                }
                return true;
            }
            return false;
        }
    }
}
