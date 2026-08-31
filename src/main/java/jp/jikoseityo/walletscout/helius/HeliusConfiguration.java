package jp.jikoseityo.walletscout.helius;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(HeliusProperties.class)
class HeliusConfiguration {
    @Bean
    RestClient heliusRestClient(RestClient.Builder builder, HeliusProperties properties) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return builder.baseUrl(properties.rpcUrl()).requestFactory(requestFactory).build();
    }
}
