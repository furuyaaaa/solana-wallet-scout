package jp.jikoseityo.walletscout.ranking;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RankingJob(
        UUID id,
        String status,
        int total,
        int completed,
        Instant startedAt,
        Instant completedAt,
        String error,
        List<RankedWallet> ranking
) {}

