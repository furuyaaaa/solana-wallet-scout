package jp.jikoseityo.walletscout.scan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ScanPreviewRequest(
        @NotBlank String tokenMint,
        @NotNull Instant from,
        @NotNull Instant to,
        @Min(1) @Max(100) Integer candidateLimit
) {
    public ScanPreviewRequest {
        if (candidateLimit == null) candidateLimit = 20;
    }
}

