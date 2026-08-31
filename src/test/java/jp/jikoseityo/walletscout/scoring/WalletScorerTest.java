package jp.jikoseityo.walletscout.scoring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletScorerTest {
    private final WalletScorer scorer = new WalletScorer();

    @Test
    void rewardsRepeatableHumanLikeWallet() {
        var result = scorer.score(new WalletMetrics(5, 6, 40, 12, 180, 0.02, 0.1, false));
        assertThat(result.score()).isGreaterThanOrEqualTo(90);
        assertThat(result.botRisk()).isLessThan(10);
    }

    @Test
    void penalizesBotLikeWallet() {
        var result = scorer.score(new WalletMetrics(8, 2, 1500, 100, 0.2, 0.95, 0.8, false));
        assertThat(result.botRisk()).isGreaterThanOrEqualTo(90);
        assertThat(result.score()).isLessThan(50);
    }

    @Test
    void excludesKnownExchangeOrProgram() {
        var result = scorer.score(new WalletMetrics(10, 10, 50, 20, 60, 0, 0, true));
        assertThat(result.botRisk()).isEqualTo(100);
        assertThat(result.reasons()).anyMatch(it -> it.contains("除外対象"));
    }
}

