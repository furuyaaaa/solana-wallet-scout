package jp.jikoseityo.walletscout.helius;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "helius")
public record HeliusProperties(String rpcUrl, String apiKey) {}

