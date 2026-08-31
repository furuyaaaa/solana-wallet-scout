package jp.jikoseityo.walletscout.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WalletHistoryAnalyzerTest {
    private final WalletHistoryAnalyzer analyzer = new WalletHistoryAnalyzer();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void calculatesActivityAndHoldingCycleMetrics() throws Exception {
        var transactions = mapper.readTree("""
                [
                  {"blockTime":100,"meta":{"preTokenBalances":[],"postTokenBalances":[
                    {"owner":"wallet","mint":"TOKEN","uiTokenAmount":{"uiAmountString":"10"}}]}},
                  {"blockTime":1300,"meta":{"preTokenBalances":[
                    {"owner":"wallet","mint":"TOKEN","uiTokenAmount":{"uiAmountString":"10"}}],
                    "postTokenBalances":[{"owner":"wallet","mint":"TOKEN","uiTokenAmount":{"uiAmountString":"0"}}]}}
                ]
                """);

        var result = analyzer.analyze("wallet", Instant.ofEpochSecond(0), Instant.ofEpochSecond(2000), transactions);

        assertThat(result.distinctTokens()).isEqualTo(1);
        assertThat(result.completedHoldingCycles()).isEqualTo(1);
        assertThat(result.medianHoldingMinutes()).isEqualTo(20);
        assertThat(result.rapidTradeRatio()).isZero();
        assertThat(result.profitabilityStatus()).contains("REQUIRES_HISTORICAL_PRICES");
    }
}
