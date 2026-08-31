package jp.jikoseityo.walletscout.helius;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class HeliusClient {
    private final RestClient restClient;
    private final HeliusProperties properties;

    public HeliusClient(RestClient heliusRestClient, HeliusProperties properties) {
        this.restClient = heliusRestClient;
        this.properties = properties;
    }

    public JsonNode getTransactionsForAddress(String address, Instant from, Instant to, int limit) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("HELIUS_API_KEY is not configured");
        }

        Map<String, Object> config = Map.of(
                "transactionDetails", "full",
                "encoding", "jsonParsed",
                "maxSupportedTransactionVersion", 0,
                "sortOrder", "asc",
                "limit", Math.min(limit, 1000),
                "filters", Map.of(
                        "blockTime", Map.of("gte", from.getEpochSecond(), "lte", to.getEpochSecond()),
                        "status", "succeeded"
                )
        );
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0",
                "id", "wallet-scout",
                "method", "getTransactionsForAddress",
                "params", List.of(address, config)
        );

        JsonNode response = restClient.post()
                .uri(uri -> uri.path("/").queryParam("api-key", properties.apiKey()).build())
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) throw new IllegalStateException("Empty response from Helius");
        if (response.hasNonNull("error")) {
            throw new IllegalStateException("Helius error: " + response.path("error").path("message").asText());
        }
        return response.path("result").path("data");
    }

    public BigDecimal getTokenSupply(String mint) {
        JsonNode result = rpc("getTokenSupply", List.of(mint));
        return new BigDecimal(result.path("value").path("uiAmountString").asText("0"));
    }

    public Map<String, String> getAccountOwners(List<String> addresses) {
        if (addresses.isEmpty()) return Map.of();
        JsonNode values = rpc("getMultipleAccounts", List.of(addresses, Map.of("encoding", "jsonParsed")))
                .path("value");
        Map<String, String> owners = new LinkedHashMap<>();
        for (int index = 0; index < addresses.size(); index++) {
            JsonNode account = values.path(index);
            owners.put(addresses.get(index), account.isMissingNode() || account.isNull()
                    ? "UNKNOWN" : account.path("owner").asText("UNKNOWN"));
        }
        return owners;
    }

    private JsonNode rpc(String method, List<?> params) {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("HELIUS_API_KEY is not configured");
        }
        Map<String, Object> request = Map.of(
                "jsonrpc", "2.0", "id", "wallet-scout", "method", method, "params", params);
        JsonNode response = restClient.post()
                .uri(uri -> uri.path("/").queryParam("api-key", properties.apiKey()).build())
                .body(request).retrieve().body(JsonNode.class);
        if (response == null) throw new IllegalStateException("Empty response from Helius");
        if (response.hasNonNull("error")) {
            throw new IllegalStateException("Helius error: " + response.path("error").path("message").asText());
        }
        return response.path("result");
    }
}
