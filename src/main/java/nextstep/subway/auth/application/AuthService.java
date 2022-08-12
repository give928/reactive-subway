package nextstep.subway.auth.application;

import lombok.RequiredArgsConstructor;
import nextstep.subway.auth.domain.LoginMember;
import nextstep.subway.auth.dto.TokenRequest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.auth.infrastructure.JwtTokenProvider;
import nextstep.subway.common.annotation.Loggable;
import nextstep.subway.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    // @formatter:off
    @Loggable
    public Mono<TokenResponse> login(TokenRequest request) {
        return memberRepository.findByEmail(request.getEmail())
                .switchIfEmpty(Mono.defer(() -> Mono.error(new AuthorizationException())))
                .map(member -> member.checkPassword(request.getPassword()))
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(member -> Mono.fromCallable(() -> jwtTokenProvider.createToken(member.getEmail()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(TokenResponse::new);
    }
    // @formatter:on

    // @formatter:off
    public Mono<LoginMember> findMemberByToken(String credentials) {
        return Mono.fromCallable(() -> jwtTokenProvider.validateToken(credentials))
                .subscribeOn(Schedulers.boundedElastic())
                .map(valid -> {
                    if (Boolean.FALSE.equals(valid)) {
                        throw new AuthorizationException();
                    }
                    return jwtTokenProvider.getPayload(credentials);
                })
                .onErrorResume(throwable -> Mono.defer(() -> Mono.error(throwable)))
                .flatMap(memberRepository::findByEmail)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .map(member -> new LoginMember(member.getId(), member.getEmail(), member.getAge()));
    }
    // @formatter:on
}
