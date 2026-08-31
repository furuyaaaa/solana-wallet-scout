package jp.jikoseityo.walletscout.wallet;

import jp.jikoseityo.walletscout.helius.HeliusClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/wallets")
public class WalletAnalysisController {
    private final HeliusClient heliusClient;
    private final WalletHistoryAnalyzer analyzer;

    public WalletAnalysisController(HeliusClient heliusClient, WalletHistoryAnalyzer analyzer) {
        this.heliusClient = heliusClient;
        this.analyzer = analyzer;
    }

    @GetMapping("/{walletAddress}/analysis")
    public WalletHistorySummary analyze(
            @PathVariable String walletAddress,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        if (days < 1 || days > 90) throw new IllegalArgumentException("days must be between 1 and 90");
        Instant end = to == null ? Instant.now() : to;
        Instant start = end.minus(days, ChronoUnit.DAYS);
        var transactions = heliusClient.getWalletTransactions(walletAddress, start, end, 1000);
        return analyzer.analyze(walletAddress, start, end, transactions);
    }
}

