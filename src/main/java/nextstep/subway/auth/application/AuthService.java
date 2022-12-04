package nextstep.subway.auth.application;

import lombok.extern.slf4j.Slf4j;
import nextstep.subway.auth.domain.LoginMember;
import nextstep.subway.auth.dto.TokenRequest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.auth.infrastructure.JwtTokenProvider;
import nextstep.subway.common.log.annotation.Loggable;
import nextstep.subway.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
@Slf4j
public class AuthService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // @formatter:off
    @Loggable
    public Mono<TokenResponse> login(TokenRequest request) {
        return memberRepository.findByEmail(request.getEmail())
                .doOnNext(member -> member.checkPassword(request.getPassword()))
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(member -> jwtTokenProvider.createToken(member.getEmail()))
                .map(TokenResponse::new)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new AuthorizationException())));
    }
    // @formatter:on

    // @formatter:off
    public Mono<LoginMember> findMemberByToken(String credentials) {
        return jwtTokenProvider.validateToken(credentials)
                .filter(valid -> valid)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new AuthorizationException())))
                .flatMap(valid -> jwtTokenProvider.getPayload(credentials))
                .flatMap(memberRepository::findByEmail)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .map(member -> LoginMember.of(member.getId(), member.getEmail(), member.getAge()));
    }
    // @formatter:on
}
