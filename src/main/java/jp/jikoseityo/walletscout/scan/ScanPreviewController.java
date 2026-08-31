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

    public ScanPreviewController(HeliusClient heliusClient, EarlyBuyerExtractor extractor) {
        this.heliusClient = heliusClient;
        this.extractor = extractor;
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(@Valid @RequestBody ScanPreviewRequest request) {
        if (!request.from().isBefore(request.to())) {
            throw new IllegalArgumentException("from must be before to");
        }
        var transactions = heliusClient.getTransactionsForAddress(
                request.tokenMint(), request.from(), request.to(), 1000);
        var buyers = extractor.extract(transactions, request.tokenMint(), request.candidateLimit());
        return Map.of(
                "tokenMint", request.tokenMint(),
                "transactionCount", transactions.size(),
                "candidateCount", buyers.size(),
                "candidates", buyers
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}

