package nextstep.subway.auth.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${security.jwt.token.secret-key}")
    private String secretKey;
    @Value("${security.jwt.token.expire-length}")
    private long validityInMilliseconds;

    private SecretKey key;

    // @formatter:off
    @PostConstruct
    protected void init() {
        String secret = Base64.getEncoder()
                .encodeToString(secretKey.getBytes());
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    // @formatter:on

    // @formatter:off
    public Mono<String> createToken(String payload) {
        Claims claims = Jwts.claims()
                .setSubject(payload);
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Mono.fromCallable(() -> Jwts.builder()
                        .setClaims(claims)
                        .setIssuedAt(now)
                        .setExpiration(validity)
                        .signWith(key, SignatureAlgorithm.HS256)
                        .compact())
                .subscribeOn(Schedulers.boundedElastic());
    }
    // @formatter:on

    // @formatter:off
    public Mono<String> getPayload(String token) {
        return Mono.just(Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject());
    }
    // @formatter:on

    // @formatter:off
    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> Jwts.parserBuilder().setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .after(new Date()))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(throwable -> Mono.just(Boolean.FALSE));
    }
    // @formatter:on
}
