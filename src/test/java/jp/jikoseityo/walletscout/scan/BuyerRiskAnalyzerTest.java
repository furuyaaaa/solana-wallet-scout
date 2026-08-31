package jp.jikoseityo.walletscout.scan;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BuyerRiskAnalyzerTest {
    private final BuyerRiskAnalyzer analyzer = new BuyerRiskAnalyzer();

    @Test
    void excludesProgramOwnedAndLargeInitialAllocations() {
        Instant time = Instant.parse("2026-08-31T00:00:00Z");
        var buyers = List.of(
                new EarlyBuyer(1, "pool", time, new BigDecimal("200"), "sig1"),
                new EarlyBuyer(2, "whale", time, new BigDecimal("150"), "sig2"),
                new EarlyBuyer(3, "human", time.plusSeconds(2), new BigDecimal("5"), "sig3")
        );
        var result = analyzer.analyze(buyers, new BigDecimal("1000"), Map.of(
                "pool", "amm-program", "whale", BuyerRiskAnalyzer.SYSTEM_PROGRAM,
                "human", BuyerRiskAnalyzer.SYSTEM_PROGRAM));

        assertThat(result).extracting(BuyerAssessment::eligible).containsExactly(false, false, true);
        assertThat(result.get(0).riskFlags()).anyMatch(flag -> flag.contains("owner program"));
        assertThat(result.get(1).riskFlags()).anyMatch(flag -> flag.contains("10%以上"));
        assertThat(result.get(2).supplyPercentage()).isEqualByComparingTo("0.5000");
    }

    @Test
    void flagsUnknownOwnerWithoutAutomaticallyExcludingIt() {
        var buyer = new EarlyBuyer(1, "closed-wallet", Instant.parse("2026-08-31T00:00:00Z"),
                new BigDecimal("1"), "sig");
        var result = analyzer.analyze(List.of(buyer), new BigDecimal("1000"),
                Map.of("closed-wallet", "UNKNOWN"));

        assertThat(result.getFirst().eligible()).isTrue();
        assertThat(result.getFirst().riskFlags()).anyMatch(flag -> flag.contains("要確認"));
    }
}
