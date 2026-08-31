package jp.jikoseityo.walletscout.scoring;

import java.util.List;

public record WalletScore(double score, double botRisk, List<String> reasons) {}

