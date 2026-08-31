package jp.jikoseityo.walletscout.scan;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BuyerRiskAnalyzer {
    static final String SYSTEM_PROGRAM = "11111111111111111111111111111111";

    public List<BuyerAssessment> analyze(
            List<EarlyBuyer> buyers, BigDecimal totalSupply, Map<String, String> accountOwners) {
        if (buyers.isEmpty()) return List.of();
        var firstSecond = buyers.getFirst().firstBuyAt();
        var result = new ArrayList<BuyerAssessment>();

        for (EarlyBuyer buyer : buyers) {
            var flags = new ArrayList<String>();
            String ownerProgram = accountOwners.getOrDefault(buyer.walletAddress(), "UNKNOWN");
            BigDecimal percentage = totalSupply.signum() == 0 ? BigDecimal.ZERO
                    : buyer.tokenAmount().multiply(BigDecimal.valueOf(100))
                    .divide(totalSupply, 4, RoundingMode.HALF_UP);

            boolean ownerUnknown = "UNKNOWN".equals(ownerProgram);
            boolean programOwned = !ownerUnknown && !SYSTEM_PROGRAM.equals(ownerProgram);
            boolean initialAllocation = percentage.compareTo(BigDecimal.TEN) >= 0;
            boolean launchSecond = buyer.firstBuyAt().equals(firstSecond);

            if (programOwned) flags.add("通常ウォレットではない可能性（owner program: " + ownerProgram + "）");
            if (ownerUnknown) flags.add("アカウント情報を取得できないため要確認");
            if (initialAllocation) flags.add("総供給量の10%以上を受領した初期配布候補");
            if (launchSecond) flags.add("最初の取引と同一秒のため、開発者・LP・スナイパー候補");

            boolean eligible = !programOwned && !initialAllocation;
            result.add(new BuyerAssessment(
                    buyer.rank(), buyer.walletAddress(), buyer.firstBuyAt(), buyer.tokenAmount(),
                    percentage, eligible, List.copyOf(flags), buyer.transactionSignature()));
        }
        return result;
    }
}
