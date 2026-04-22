package com.digitaltherapy.service.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseLoaderTest {

    @Mock
    private VectorStore vectorStore;

    @TempDir
    Path tempDir;

    @Test
    void loadKnowledgeBase_StoreFileExists_SkipsLoading() throws IOException {
        // given
        File storeFile = tempDir.resolve("vector-store.json").toFile();
        Files.writeString(storeFile.toPath(), "{\"vectors\": []}");

        KnowledgeBaseLoader loader = new KnowledgeBaseLoader(vectorStore, storeFile.getAbsolutePath());

        // when
        loader.loadKnowledgeBase();

        // then
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void loadKnowledgeBase_StoreFileNotExists_LoadsDocuments() {
        // given
        String nonExistentPath = tempDir.resolve("non-existent-store.json").toString();
        KnowledgeBaseLoader loader = new KnowledgeBaseLoader(vectorStore, nonExistentPath);

        // when
        loader.loadKnowledgeBase();

        // then - should call vectorStore.add at least 3 times (distortions, techniques, protocols)
        verify(vectorStore, atLeast(3)).add(anyList());
    }

    @Test
    void loadKnowledgeBase_LoadsDistortionsWithMetadata() {
        // given
        String nonExistentPath = tempDir.resolve("non-existent-store.json").toString();
        KnowledgeBaseLoader loader = new KnowledgeBaseLoader(vectorStore, nonExistentPath);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);

        // when
        loader.loadKnowledgeBase();

        // then
        verify(vectorStore, atLeast(1)).add(captor.capture());

        List<List<Document>> allAddCalls = captor.getAllValues();
        boolean foundDistortion = allAddCalls.stream()
                .flatMap(List::stream)
                .anyMatch(doc -> "distortion".equals(doc.getMetadata().get("type")));
        assertThat(foundDistortion).isTrue();
    }

    @Test
    void loadKnowledgeBase_HandlesExceptionGracefully() {
        // given
        String nonExistentPath = tempDir.resolve("non-existent-store.json").toString();
        KnowledgeBaseLoader loader = new KnowledgeBaseLoader(vectorStore, nonExistentPath);

        doThrow(new RuntimeException("Vector store failure"))
                .when(vectorStore).add(anyList());

        // when / then - no exception should propagate
        assertThatCode(() -> loader.loadKnowledgeBase()).doesNotThrowAnyException();
    }

    @Test
    void loadKnowledgeBase_EmptyStoreFile_LoadsDocuments() throws IOException {
        // given
        File emptyFile = tempDir.resolve("empty-store.json").toFile();
        emptyFile.createNewFile(); // creates a 0-byte file

        KnowledgeBaseLoader loader = new KnowledgeBaseLoader(vectorStore, emptyFile.getAbsolutePath());

        // when
        loader.loadKnowledgeBase();

        // then - empty file (length == 0) should trigger loading
        verify(vectorStore, atLeast(3)).add(anyList());
    }
}
