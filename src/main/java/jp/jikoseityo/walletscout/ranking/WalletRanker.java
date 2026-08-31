package jp.jikoseityo.walletscout.ranking;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class WalletRanker {
    public List<RankedWallet> rank(List<RankedWallet> wallets) {
        var sorted = wallets.stream()
                .sorted(Comparator.comparingDouble((RankedWallet item) -> item.history().activityScore()).reversed()
                        .thenComparingDouble(item -> item.history().botRisk())
                        .thenComparing(item -> item.buyer().firstBuyAt()))
                .toList();
        return java.util.stream.IntStream.range(0, sorted.size())
                .mapToObj(index -> sorted.get(index).withRank(index + 1)).toList();
    }
}

