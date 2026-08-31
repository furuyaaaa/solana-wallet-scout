package jp.jikoseityo.walletscout.scan;

import jakarta.validation.Valid;
import jp.jikoseityo.walletscout.helius.HeliusClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/scans")
public class ScanPreviewController {
    private final HeliusClient heliusClient;
    private final EarlyBuyerExtractor extractor;
    private final BuyerRiskAnalyzer riskAnalyzer;

    public ScanPreviewController(HeliusClient heliusClient, EarlyBuyerExtractor extractor,
                                 BuyerRiskAnalyzer riskAnalyzer) {
        this.heliusClient = heliusClient;
        this.extractor = extractor;
        this.riskAnalyzer = riskAnalyzer;
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(@Valid @RequestBody ScanPreviewRequest request) {
        if (!request.from().isBefore(request.to())) {
            throw new IllegalArgumentException("from must be before to");
        }
        var transactions = heliusClient.getTransactionsForAddress(
                request.tokenMint(), request.from(), request.to(), 1000);
        int rawLimit = Math.min(40, Math.max(request.candidateLimit() * 2, request.candidateLimit()));
        var buyers = extractor.extract(transactions, request.tokenMint(), rawLimit);
        var supply = heliusClient.getTokenSupply(request.tokenMint());
        var owners = heliusClient.getAccountOwners(buyers.stream().map(EarlyBuyer::walletAddress).toList());
        var assessed = riskAnalyzer.analyze(buyers, supply, owners);
        var eligible = assessed.stream().filter(BuyerAssessment::eligible)
                .limit(request.candidateLimit()).toList();
        return Map.of(
                "tokenMint", request.tokenMint(),
                "transactionCount", transactions.size(),
                "tokenSupply", supply,
                "rawCandidateCount", assessed.size(),
                "candidateCount", eligible.size(),
                "candidates", eligible,
                "excluded", assessed.stream().filter(candidate -> !candidate.eligible()).toList()
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
