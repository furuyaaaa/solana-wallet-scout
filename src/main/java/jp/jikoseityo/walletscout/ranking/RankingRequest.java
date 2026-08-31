package jp.jikoseityo.walletscout.ranking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RankingRequest(
        @NotBlank String tokenMint,
        @NotNull Instant launchFrom,
        @NotNull Instant launchTo,
        @Min(1) @Max(20) Integer candidateLimit,
        @Min(1) @Max(90) Integer historyDays,
        Instant historyTo
) {
    public RankingRequest {
        if (candidateLimit == null) candidateLimit = 20;
        if (historyDays == null) historyDays = 30;
        if (historyTo == null) historyTo = Instant.now();
    }
}

