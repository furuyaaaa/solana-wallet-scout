package jp.jikoseityo.walletscout.scan;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BuyerAssessment(
        int rank,
        String walletAddress,
        Instant firstBuyAt,
        BigDecimal tokenAmount,
        BigDecimal supplyPercentage,
        boolean eligible,
        List<String> riskFlags,
        String transactionSignature
) {}

