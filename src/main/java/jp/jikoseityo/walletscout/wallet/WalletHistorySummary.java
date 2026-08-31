package jp.jikoseityo.walletscout.wallet;

import java.time.Instant;
import java.util.List;

public record WalletHistorySummary(
        String walletAddress,
        Instant from,
        Instant to,
        int transactions,
        int activeDays,
        int distinctTokens,
        int completedHoldingCycles,
        double medianHoldingMinutes,
        double rapidTradeRatio,
        double botRisk,
        double activityScore,
        boolean truncated,
        String profitabilityStatus,
        List<String> reasons
) {}

