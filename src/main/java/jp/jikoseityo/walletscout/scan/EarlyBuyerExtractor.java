package jp.jikoseityo.walletscout.scan;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EarlyBuyerExtractor {
    public java.util.List<EarlyBuyer> extract(JsonNode transactions, String mint, int limit) {
        Map<String, Candidate> candidates = new LinkedHashMap<>();
        for (JsonNode tx : transactions) {
            JsonNode meta = tx.path("meta");
            Map<String, BigDecimal> before = balancesByOwner(meta.path("preTokenBalances"), mint);
            Map<String, BigDecimal> after = balancesByOwner(meta.path("postTokenBalances"), mint);

            for (Map.Entry<String, BigDecimal> entry : after.entrySet()) {
                String owner = entry.getKey();
                BigDecimal increase = entry.getValue().subtract(before.getOrDefault(owner, BigDecimal.ZERO));
                if (increase.signum() <= 0 || candidates.containsKey(owner)) continue;

                String signature = tx.path("transaction").path("signatures").path(0).asText();
                candidates.put(owner, new Candidate(
                        owner,
                        Instant.ofEpochSecond(tx.path("blockTime").asLong()),
                        increase,
                        signature
                ));
                if (candidates.size() >= limit) return ranked(candidates);
            }
        }
        return ranked(candidates);
    }

    private Map<String, BigDecimal> balancesByOwner(JsonNode balances, String mint) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (JsonNode balance : balances) {
            if (!mint.equals(balance.path("mint").asText())) continue;
            String owner = balance.path("owner").asText();
            if (owner.isBlank()) continue;
            BigDecimal amount = new BigDecimal(balance.path("uiTokenAmount").path("uiAmountString").asText("0"));
            result.merge(owner, amount, BigDecimal::add);
        }
        return result;
    }

    private java.util.List<EarlyBuyer> ranked(Map<String, Candidate> candidates) {
        var result = new ArrayList<EarlyBuyer>();
        int rank = 1;
        for (Candidate c : candidates.values()) {
            result.add(new EarlyBuyer(rank++, c.owner(), c.time(), c.amount(), c.signature()));
        }
        return result;
    }

    private record Candidate(String owner, Instant time, BigDecimal amount, String signature) {}
}

