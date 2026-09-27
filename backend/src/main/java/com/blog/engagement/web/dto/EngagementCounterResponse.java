package com.blog.engagement.web.dto;

import com.blog.engagement.projection.EngagementCounter;

import java.time.Instant;

// os totais de engajamento de um post, como a estante consome. o updatedAt e o instante
// da ultima mudanca conhecida na origem: e a idade da informacao, e deixa explicito que
// ela e uma copia.
public record EngagementCounterResponse(
        Long postId,
        long comments,
        long reactions,
        Instant updatedAt
) {
    public static EngagementCounterResponse from(EngagementCounter counter) {
        return new EngagementCounterResponse(
                counter.getPostId(),
                counter.getComments(),
                counter.getReactions(),
                counter.getUpdatedAt()
        );
    }
}
