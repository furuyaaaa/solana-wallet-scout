package jp.jikoseityo.walletscout.ranking;

import jp.jikoseityo.walletscout.helius.HeliusClient;
import jp.jikoseityo.walletscout.scan.BuyerAssessment;
import jp.jikoseityo.walletscout.scan.BuyerRiskAnalyzer;
import jp.jikoseityo.walletscout.scan.EarlyBuyer;
import jp.jikoseityo.walletscout.scan.EarlyBuyerExtractor;
import jp.jikoseityo.walletscout.wallet.WalletHistoryAnalyzer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RankingService {
    private final HeliusClient helius;
    private final EarlyBuyerExtractor extractor;
    private final BuyerRiskAnalyzer riskAnalyzer;
    private final WalletHistoryAnalyzer historyAnalyzer;
    private final WalletRanker ranker;
    private final ConcurrentHashMap<UUID, RankingJob> jobs = new ConcurrentHashMap<>();

    public RankingService(HeliusClient helius, EarlyBuyerExtractor extractor, BuyerRiskAnalyzer riskAnalyzer,
                          WalletHistoryAnalyzer historyAnalyzer, WalletRanker ranker) {
        this.helius = helius;
        this.extractor = extractor;
        this.riskAnalyzer = riskAnalyzer;
        this.historyAnalyzer = historyAnalyzer;
        this.ranker = ranker;
    }

    public RankingJob start(RankingRequest request) {
        if (!request.launchFrom().isBefore(request.launchTo())) {
            throw new IllegalArgumentException("launchFrom must be before launchTo");
        }
        UUID id = UUID.randomUUID();
        RankingJob queued = new RankingJob(id, "QUEUED", request.candidateLimit(), 0,
                Instant.now(), null, null, List.of());
        jobs.put(id, queued);
        CompletableFuture.runAsync(() -> execute(id, request));
        return queued;
    }

    public RankingJob get(UUID id) {
        RankingJob job = jobs.get(id);
        if (job == null) throw new NoSuchElementException("Ranking job not found");
        return job;
    }

    private void execute(UUID id, RankingRequest request) {
        var results = new ArrayList<RankedWallet>();
        try {
            jobs.computeIfPresent(id, (key, old) -> update(old, "SCANNING", old.total(), 0, null, results));
            var transactions = helius.getTransactionsForAddress(
                    request.tokenMint(), request.launchFrom(), request.launchTo(), 1000);
            int rawLimit = Math.min(40, request.candidateLimit() * 2);
            List<EarlyBuyer> raw = extractor.extract(transactions, request.tokenMint(), rawLimit);
            var supply = helius.getTokenSupply(request.tokenMint());
            var owners = helius.getAccountOwners(raw.stream().map(EarlyBuyer::walletAddress).toList());
            List<BuyerAssessment> candidates = riskAnalyzer.analyze(raw, supply, owners).stream()
                    .filter(BuyerAssessment::eligible).limit(request.candidateLimit()).toList();
            jobs.computeIfPresent(id, (key, old) -> update(old, "ANALYZING", candidates.size(), 0, null, results));

            Instant historyFrom = request.historyTo().minus(request.historyDays(), ChronoUnit.DAYS);
            for (BuyerAssessment candidate : candidates) {
                var history = helius.getWalletTransactions(
                        candidate.walletAddress(), historyFrom, request.historyTo(), 1000);
                var summary = historyAnalyzer.analyze(
                        candidate.walletAddress(), historyFrom, request.historyTo(), history);
                results.add(new RankedWallet(0, candidate, summary));
                jobs.computeIfPresent(id, (key, old) -> update(
                        old, "ANALYZING", candidates.size(), results.size(), null, ranker.rank(results)));
            }
            jobs.computeIfPresent(id, (key, old) -> new RankingJob(old.id(), "COMPLETED", old.total(),
                    old.total(), old.startedAt(), Instant.now(), null, ranker.rank(results)));
        } catch (Exception exception) {
            jobs.computeIfPresent(id, (key, old) -> new RankingJob(old.id(), "FAILED", old.total(),
                    old.completed(), old.startedAt(), Instant.now(), exception.getMessage(), ranker.rank(results)));
        }
    }

    private RankingJob update(RankingJob old, String status, int total, int completed,
                              String error, List<RankedWallet> ranking) {
        return new RankingJob(old.id(), status, total, completed, old.startedAt(), null, error, List.copyOf(ranking));
    }
}

