package nextstep.subway.member.ui;

import nextstep.subway.auth.domain.AuthenticationPrincipal;
import nextstep.subway.auth.domain.LoginMember;
import nextstep.subway.member.application.MemberService;
import nextstep.subway.member.dto.MemberRequest;
import nextstep.subway.member.dto.MemberResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;

@RequestMapping("/members")
@RestController
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> createMember(@RequestBody MemberRequest request) {
        return memberService.createMember(request)
                .map(memberResponse -> ResponseEntity.created(URI.create("/members/" + memberResponse.getId()))
                        .build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<MemberResponse>> findMember(@PathVariable Long id) {
        return memberService.findMember(id)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Void>> updateMember(@PathVariable Long id, @RequestBody MemberRequest param) {
        return memberService.updateMember(id, param)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteMember(@PathVariable Long id) {
        return memberService.deleteMember(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<MemberResponse>> findMemberOfMine(@AuthenticationPrincipal LoginMember loginMember) {
        return memberService.findMember(loginMember.getId())
                .map(ResponseEntity::ok);
    }

    @PutMapping("/me")
    public Mono<ResponseEntity<Void>> updateMemberOfMine(@AuthenticationPrincipal LoginMember loginMember, @RequestBody MemberRequest param) {
        return memberService.updateMember(loginMember.getId(), param)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/me")
    public Mono<ResponseEntity<Void>> deleteMemberOfMine(@AuthenticationPrincipal LoginMember loginMember) {
        return memberService.deleteMember(loginMember.getId())
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
