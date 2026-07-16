package com.zhifutong.customer.config;

import com.zhifutong.customer.client.ChatModelClient;
import com.zhifutong.customer.client.DeterministicEmbeddingClient;
import com.zhifutong.customer.client.EmbeddingClient;
import com.zhifutong.customer.client.LocalOnnxEmbeddingClient;
import com.zhifutong.customer.client.MockChatModelClient;
import com.zhifutong.customer.client.OpenAiCompatibleChatModelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfig {

    @Bean
    public EmbeddingClient embeddingClient(AppProperties properties) {
        if (properties.getEmbedding().isMockEnabled()) {
            return new DeterministicEmbeddingClient(properties);
        }
        return new LocalOnnxEmbeddingClient(properties);
    }

    @Bean
    public ChatModelClient chatModelClient(AppProperties properties, WebClient.Builder builder) {
        if (properties.getLlm().isMockEnabled() || properties.getLlm().getApiKey() == null || properties.getLlm().getApiKey().isBlank()) {
            return new MockChatModelClient();
        }
        return new OpenAiCompatibleChatModelClient(properties, builder);
    }
}
