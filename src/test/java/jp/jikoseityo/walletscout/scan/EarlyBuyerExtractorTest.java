package jp.jikoseityo.walletscout.scan;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EarlyBuyerExtractorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final EarlyBuyerExtractor extractor = new EarlyBuyerExtractor();

    @Test
    void extractsPositiveBalanceChangesInChronologicalOrderAndDeduplicatesWallets() throws Exception {
        var transactions = mapper.readTree("""
                [
                  {"blockTime":100,"transaction":{"signatures":["sig1"]},"meta":{
                    "preTokenBalances":[],
                    "postTokenBalances":[{"mint":"MINT","owner":"walletA","uiTokenAmount":{"uiAmountString":"12.5"}}]}},
                  {"blockTime":200,"transaction":{"signatures":["sig2"]},"meta":{
                    "preTokenBalances":[{"mint":"MINT","owner":"walletA","uiTokenAmount":{"uiAmountString":"12.5"}}],
                    "postTokenBalances":[{"mint":"MINT","owner":"walletA","uiTokenAmount":{"uiAmountString":"20"}}]}},
                  {"blockTime":300,"transaction":{"signatures":["sig3"]},"meta":{
                    "preTokenBalances":[],
                    "postTokenBalances":[{"mint":"MINT","owner":"walletB","uiTokenAmount":{"uiAmountString":"3"}}]}}
                ]
                """);

        var result = extractor.extract(transactions, "MINT", 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).walletAddress()).isEqualTo("walletA");
        assertThat(result.get(0).tokenAmount()).isEqualByComparingTo("12.5");
        assertThat(result.get(1).walletAddress()).isEqualTo("walletB");
    }
}
