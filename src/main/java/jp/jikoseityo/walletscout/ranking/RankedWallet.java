package jp.jikoseityo.walletscout.ranking;

import jp.jikoseityo.walletscout.scan.BuyerAssessment;
import jp.jikoseityo.walletscout.wallet.WalletHistorySummary;

public record RankedWallet(int rank, BuyerAssessment buyer, WalletHistorySummary history) {
    public RankedWallet withRank(int newRank) {
        return new RankedWallet(newRank, buyer, history);
    }
}

