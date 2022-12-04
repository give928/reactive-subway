package nextstep.subway.member.application;

import lombok.extern.slf4j.Slf4j;
import nextstep.subway.common.log.annotation.Loggable;
import nextstep.subway.member.domain.MemberRepository;
import nextstep.subway.member.dto.MemberRequest;
import nextstep.subway.member.dto.MemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@Transactional(readOnly = true)
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Loggable
    @Transactional
    public Mono<MemberResponse> createMember(MemberRequest request) {
        return memberRepository.save(request.toMember())
                .map(MemberResponse::of);
    }

    // @formatter:off
    public Mono<MemberResponse> findMember(Long id) {
        return memberRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .map(MemberResponse::of);
    }
    // @formatter:on

    // @formatter:off
    @Loggable
    @Transactional
    public Mono<Void> updateMember(Long id, MemberRequest param) {
        return memberRepository.findById(id)
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException())))
                .doOnNext(member -> member.update(param.toMember()))
                .flatMap(memberRepository::save)
                .then();
    }
    // @formatter:on

    @Loggable
    @Transactional
    public Mono<Void> deleteMember(Long id) {
        return memberRepository.deleteById(id);
    }
}
