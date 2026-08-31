package jp.jikoseityo.walletscout.scoring;

public record WalletMetrics(
        int earlyEntries,
        int distinctProfitableTokens,
        int tradesLast30Days,
        int distinctTokensLast30Days,
        double medianHoldingMinutes,
        double rapidTradeRatio,
        double fundingConcentration,
        boolean knownProgramOrExchange
) {}

