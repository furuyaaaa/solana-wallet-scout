package jp.jikoseityo.walletscout.scoring;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class WalletScorer {
    public WalletScore score(WalletMetrics m) {
        var reasons = new ArrayList<String>();
        double score = 0;

        double repeatability = Math.min(30, m.distinctProfitableTokens() * 5.0);
        score += repeatability;
        reasons.add("再現性 " + round(repeatability) + "/30");

        double earlyEntry = Math.min(25, m.earlyEntries() * 5.0);
        score += earlyEntry;
        reasons.add("初期購入実績 " + round(earlyEntry) + "/25");

        double activity = m.tradesLast30Days() >= 3 && m.tradesLast30Days() <= 200 ? 20 :
                m.tradesLast30Days() > 0 ? 8 : 0;
        score += activity;
        reasons.add("直近活動 " + round(activity) + "/20");

        double diversification = Math.min(15, m.distinctTokensLast30Days() * 1.5);
        score += diversification;
        reasons.add("取引銘柄の分散 " + round(diversification) + "/15");

        double holding = m.medianHoldingMinutes() >= 10 ? 10 : 2;
        score += holding;
        reasons.add("保有時間 " + round(holding) + "/10");

        double botRisk = Math.min(100,
                m.rapidTradeRatio() * 55 + m.fundingConcentration() * 25 +
                        (m.tradesLast30Days() > 1000 ? 20 : 0) +
                        (m.knownProgramOrExchange() ? 100 : 0));
        score -= botRisk * 0.5;
        if (m.knownProgramOrExchange()) reasons.add("取引所・既知プログラム候補のため除外対象");
        if (botRisk >= 50) reasons.add("botリスクが高い");

        return new WalletScore(round(Math.max(0, Math.min(100, score))), round(botRisk), reasons);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

