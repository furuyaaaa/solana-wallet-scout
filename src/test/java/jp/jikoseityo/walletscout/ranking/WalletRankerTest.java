package jp.jikoseityo.walletscout.ranking;

import jp.jikoseityo.walletscout.scan.BuyerAssessment;
import jp.jikoseityo.walletscout.wallet.WalletHistorySummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WalletRankerTest {
    private final WalletRanker ranker = new WalletRanker();

    @Test
    void sortsByActivityScoreThenLowerBotRisk() {
        var result = ranker.rank(List.of(item("a", 70, 40), item("b", 80, 50), item("c", 80, 10)));
        assertThat(result).extracting(item -> item.buyer().walletAddress()).containsExactly("c", "b", "a");
        assertThat(result).extracting(RankedWallet::rank).containsExactly(1, 2, 3);
    }

    private RankedWallet item(String wallet, double score, double botRisk) {
        Instant now = Instant.parse("2026-09-01T00:00:00Z");
        var buyer = new BuyerAssessment(1, wallet, now, BigDecimal.ONE, BigDecimal.ZERO,
                true, List.of(), "sig");
        var history = new WalletHistorySummary(wallet, now.minusSeconds(1), now, 1, 1, 1,
                0, 0, 0, botRisk, score, false, "NOT_CALCULATED", List.of());
        return new RankedWallet(0, buyer, history);
    }
}
