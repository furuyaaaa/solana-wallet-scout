package jp.jikoseityo.walletscout.scan;

import java.math.BigDecimal;
import java.time.Instant;

public record EarlyBuyer(
        int rank,
        String walletAddress,
        Instant firstBuyAt,
        BigDecimal tokenAmount,
        String transactionSignature
) {}

