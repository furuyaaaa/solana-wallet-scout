package jp.jikoseityo.walletscout.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Component
public class WalletHistoryAnalyzer {
    public WalletHistorySummary analyze(String wallet, Instant from, Instant to, JsonNode transactions) {
        var activeDays = new HashSet<>();
        var distinctTokens = new HashSet<String>();
        var openedAt = new HashMap<String, Instant>();
        var holdings = new HashMap<String, BigDecimal>();
        var holdingMinutes = new ArrayList<Double>();

        for (JsonNode tx : transactions) {
            Instant time = Instant.ofEpochSecond(tx.path("blockTime").asLong());
            activeDays.add(time.atZone(ZoneOffset.UTC).toLocalDate());
            Map<String, BigDecimal> before = balancesByMint(tx.path("meta").path("preTokenBalances"), wallet);
            Map<String, BigDecimal> after = balancesByMint(tx.path("meta").path("postTokenBalances"), wallet);
            var mints = new HashSet<>(before.keySet());
            mints.addAll(after.keySet());

            for (String mint : mints) {
                BigDecimal oldBalance = before.getOrDefault(mint, holdings.getOrDefault(mint, BigDecimal.ZERO));
                BigDecimal newBalance = after.getOrDefault(mint, BigDecimal.ZERO);
                if (newBalance.compareTo(oldBalance) != 0) distinctTokens.add(mint);
                if (oldBalance.signum() == 0 && newBalance.signum() > 0) openedAt.putIfAbsent(mint, time);
                if (oldBalance.signum() > 0 && newBalance.signum() == 0) {
                    Instant opened = openedAt.remove(mint);
                    if (opened != null) holdingMinutes.add(Duration.between(opened, time).toSeconds() / 60.0);
                }
                holdings.put(mint, newBalance);
            }
        }

        double median = median(holdingMinutes);
        long rapid = holdingMinutes.stream().filter(minutes -> minutes < 10).count();
        double rapidRatio = holdingMinutes.isEmpty() ? 0 : (double) rapid / holdingMinutes.size();
        double botRisk = Math.min(100,
                rapidRatio * 60 + (transactions.size() >= 1000 ? 20 : 0)
                        + (activeDays.isEmpty() ? 20 : Math.max(0, transactions.size() / (double) activeDays.size() - 100) * 0.2));

        double activity = 0;
        activity += transactions.size() >= 3 && transactions.size() <= 500 ? 25 : transactions.size() > 0 ? 10 : 0;
        activity += Math.min(25, distinctTokens.size() * 2.5);
        activity += Math.min(20, activeDays.size() * 2.0);
        activity += holdingMinutes.isEmpty() ? 5 : median >= 10 ? 20 : 8;
        activity += Math.max(0, 10 - rapidRatio * 10);
        activity -= botRisk * 0.35;
        activity = round(Math.max(0, Math.min(100, activity)));

        var reasons = new ArrayList<String>();
        reasons.add("直近取引 " + transactions.size() + "件");
        reasons.add("活動日数 " + activeDays.size() + "日");
        reasons.add("取引銘柄 " + distinctTokens.size() + "種類");
        if (!holdingMinutes.isEmpty()) reasons.add("保有時間中央値 " + round(median) + "分");
        if (rapidRatio >= 0.5) reasons.add("10分未満の短時間売買が多い");
        if (transactions.size() >= 1000) reasons.add("上限1000件に達したため履歴が一部省略");

        return new WalletHistorySummary(wallet, from, to, transactions.size(), activeDays.size(),
                distinctTokens.size(), holdingMinutes.size(), round(median), round(rapidRatio),
                round(botRisk), activity, transactions.size() >= 1000,
                "NOT_CALCULATED_REQUIRES_HISTORICAL_PRICES", List.copyOf(reasons));
    }

    private Map<String, BigDecimal> balancesByMint(JsonNode balances, String wallet) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (JsonNode balance : balances) {
            if (!wallet.equals(balance.path("owner").asText())) continue;
            String mint = balance.path("mint").asText();
            BigDecimal amount = new BigDecimal(balance.path("uiTokenAmount").path("uiAmountString").asText("0"));
            result.merge(mint, amount, BigDecimal::add);
        }
        return result;
    }

    private double median(List<Double> values) {
        if (values.isEmpty()) return 0;
        var sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1 ? sorted.get(middle) : (sorted.get(middle - 1) + sorted.get(middle)) / 2;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

