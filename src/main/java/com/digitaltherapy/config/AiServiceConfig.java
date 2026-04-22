package com.digitaltherapy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.File;

@Configuration
@Slf4j
public class AiServiceConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel,
                                         @Value("${spring.ai.vectorstore.simple.store.path:./data/vectors/vector-store.json}") String storePath) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File storeFile = new File(storePath);
        if (storeFile.exists()) {
            log.info("Loading vector store from: {}", storeFile.getAbsolutePath());
            store.load(storeFile);
            log.info("Vector store loaded with existing embeddings");
        } else {
            log.info("No existing vector store file at: {}. Will be created after knowledge base loading.", storeFile.getAbsolutePath());
        }
        return store;
    }
}
