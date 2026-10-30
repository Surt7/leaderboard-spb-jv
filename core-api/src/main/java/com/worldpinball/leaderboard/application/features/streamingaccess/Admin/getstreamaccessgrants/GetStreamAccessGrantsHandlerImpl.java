package com.worldpinball.leaderboard.application.features.streamingaccess.Admin.getstreamaccessgrants;

import com.worldpinball.leaderboard.infrastructure.StreamAccessGrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetStreamAccessGrantsHandlerImpl implements GetStreamAccessGrantsHandler {

    private final StreamAccessGrantRepository repository;

    public GetStreamAccessGrantsHandlerImpl(StreamAccessGrantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public GetStreamAccessGrantsResult handle(GetStreamAccessGrantsQuery query) {
        List<StreamAccessGrantDto> dtos = repository.findAllByOrderByCreatedAtUtcDesc()
                .stream()
                .map(g -> new StreamAccessGrantDto(
                        g.getGrantId(),
                        g.getPrincipalType(),
                        g.getPrincipalId(),
                        g.getDisplayName(),
                        g.getOrigin(),
                        g.getStatus(),
                        g.getCreatedAtUtc(),
                        g.getExpiresAtUtc(),
                        g.getRevokedAtUtc(),
                        g.getRevocationReason(),
                        g.getGrantedByUserId()
                ))
                .toList();

        return new GetStreamAccessGrantsResult.Success(dtos);
    }
}
