package jp.jikoseityo.walletscout.helius;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(HeliusProperties.class)
class HeliusConfiguration {
    @Bean
    RestClient heliusRestClient(RestClient.Builder builder, HeliusProperties properties) {
        return builder.baseUrl(properties.rpcUrl()).build();
    }
}

