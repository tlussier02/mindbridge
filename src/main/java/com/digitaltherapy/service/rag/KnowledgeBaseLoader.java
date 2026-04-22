package com.digitaltherapy.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class KnowledgeBaseLoader {

    private final VectorStore vectorStore;
    private final String storePath;

    public KnowledgeBaseLoader(VectorStore vectorStore,
                               @Value("${spring.ai.vectorstore.simple.store.path:./data/vectors/mvvector-store.json}") String storePath) {
        this.vectorStore = vectorStore;
        this.storePath = storePath;
    }

    @PostConstruct
    public void loadKnowledgeBase() {
        File storeFile = new File(storePath);
        if (storeFile.exists() && storeFile.length() > 0) {
            log.info("Vector store already persisted at {}. Skipping knowledge base reload.", storeFile.getAbsolutePath());
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        int totalLoaded = 0;

        totalLoaded += loadDistortions(mapper);
        totalLoaded += loadCbtTechniques(mapper);
        totalLoaded += loadCrisisProtocols(mapper);

        log.info("Knowledge base loaded: {} total documents in vector store", totalLoaded);

        // Persist to disk
        if (vectorStore instanceof SimpleVectorStore svs) {
            storeFile.getParentFile().mkdirs();
            svs.save(storeFile);
            log.info("Vector store persisted to: {}", storeFile.getAbsolutePath());
        }
    }

    private int loadDistortions(ObjectMapper mapper) {
        int count = 0;
        try {
            InputStream is = new ClassPathResource("knowledge-base/distortions.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            JsonNode distortions = root.get("distortions");

            if (distortions != null && distortions.isArray()) {
                for (JsonNode distortion : distortions) {
                    String id = distortion.get("id").asText();
                    String name = distortion.get("name").asText();
                    String description = distortion.get("description").asText();

                    StringBuilder content = new StringBuilder();
                    content.append("Cognitive Distortion: ").append(name).append("\n");
                    content.append("Description: ").append(description).append("\n");

                    JsonNode examples = distortion.get("examples");
                    if (examples != null && examples.isArray()) {
                        content.append("Examples:\n");
                        for (JsonNode example : examples) {
                            content.append("- ").append(example.asText()).append("\n");
                        }
                    }

                    JsonNode challengeQuestions = distortion.get("challengeQuestions");
                    if (challengeQuestions != null && challengeQuestions.isArray()) {
                        content.append("Challenge Questions:\n");
                        for (JsonNode question : challengeQuestions) {
                            content.append("- ").append(question.asText()).append("\n");
                        }
                    }

                    JsonNode reframingStrategies = distortion.get("reframingStrategies");
                    if (reframingStrategies != null && reframingStrategies.isArray()) {
                        content.append("Reframing Strategies:\n");
                        for (JsonNode strategy : reframingStrategies) {
                            content.append("- ").append(strategy.asText()).append("\n");
                        }
                    }

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("type", "distortion");
                    metadata.put("id", id);
                    metadata.put("name", name);

                    Document doc = new Document("distortion-" + id, content.toString(), metadata);
                    vectorStore.add(List.of(doc));
                    count++;
                }
            }
            log.info("Loaded {} cognitive distortion documents", count);
        } catch (Exception e) {
            log.warn("Failed to load distortions knowledge base: {}", e.getMessage());
        }
        return count;
    }

    private int loadCbtTechniques(ObjectMapper mapper) {
        int count = 0;
        try {
            InputStream is = new ClassPathResource("knowledge-base/cbt-techniques.json").getInputStream();
            JsonNode root = mapper.readTree(is);
            JsonNode techniques = root.get("techniques");

            if (techniques != null && techniques.isArray()) {
                for (JsonNode technique : techniques) {
                    String id = technique.get("id").asText();
                    String name = technique.get("name").asText();
                    String description = technique.get("description").asText();

                    StringBuilder content = new StringBuilder();
                    content.append("CBT Technique: ").append(name).append("\n");
                    content.append("Description: ").append(description).append("\n");

                    JsonNode steps = technique.get("steps");
                    if (steps != null && steps.isArray()) {
                        content.append("Steps:\n");
                        int stepNum = 1;
                        for (JsonNode step : steps) {
                            content.append(stepNum++).append(". ").append(step.asText()).append("\n");
                        }
                    }

                    JsonNode applicableTo = technique.get("applicableTo");
                    if (applicableTo != null && applicableTo.isArray()) {
                        content.append("Applicable to distortions: ");
                        StringBuilder distortionList = new StringBuilder();
                        for (JsonNode dist : applicableTo) {
                            if (distortionList.length() > 0) distortionList.append(", ");
                            distortionList.append(dist.asText());
                        }
                        content.append(distortionList).append("\n");
                    }

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("type", "technique");
                    metadata.put("id", id);
                    metadata.put("name", name);

                    Document doc = new Document("technique-" + id, content.toString(), metadata);
                    vectorStore.add(List.of(doc));
                    count++;
                }
            }
            log.info("Loaded {} CBT technique documents", count);
        } catch (Exception e) {
            log.warn("Failed to load CBT techniques knowledge base: {}", e.getMessage());
        }
        return count;
    }

    private int loadCrisisProtocols(ObjectMapper mapper) {
        int count = 0;
        try {
            InputStream is = new ClassPathResource("knowledge-base/crisis-protocols.json").getInputStream();
            JsonNode root = mapper.readTree(is);

            // Warning Signs
            JsonNode warningSignsNode = root.get("warningSignsRecognition");
            if (warningSignsNode != null) {
                StringBuilder content = new StringBuilder();
                content.append("Crisis Warning Signs Recognition\n");
                content.append("Description: ").append(warningSignsNode.get("description").asText()).append("\n");

                appendSignList(content, "Verbal Signs", warningSignsNode.get("verbalSigns"));
                appendSignList(content, "Behavioral Signs", warningSignsNode.get("behavioralSigns"));
                appendSignList(content, "Emotional Signs", warningSignsNode.get("emotionalSigns"));

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "crisis-protocol");
                metadata.put("id", "warning-signs");
                metadata.put("name", "Warning Signs Recognition");

                Document doc = new Document("crisis-warning-signs", content.toString(), metadata);
                vectorStore.add(List.of(doc));
                count++;
            }

            // De-escalation Techniques
            JsonNode deEscalationNode = root.get("deEscalationTechniques");
            if (deEscalationNode != null && deEscalationNode.isArray()) {
                StringBuilder content = new StringBuilder();
                content.append("Crisis De-Escalation Techniques\n");

                for (JsonNode technique : deEscalationNode) {
                    content.append("- ").append(technique.get("name").asText())
                        .append(": ").append(technique.get("description").asText()).append("\n");
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "crisis-protocol");
                metadata.put("id", "de-escalation");
                metadata.put("name", "De-Escalation Techniques");

                Document doc = new Document("crisis-de-escalation", content.toString(), metadata);
                vectorStore.add(List.of(doc));
                count++;
            }

            // Safety Planning
            JsonNode safetyPlanningNode = root.get("safetyPlanningSteps");
            if (safetyPlanningNode != null && safetyPlanningNode.isArray()) {
                StringBuilder content = new StringBuilder();
                content.append("Safety Planning Steps\n");

                int stepNum = 1;
                for (JsonNode step : safetyPlanningNode) {
                    content.append(stepNum++).append(". ").append(step.asText()).append("\n");
                }

                // Include emergency resources
                JsonNode emergencyResources = root.get("emergencyResources");
                if (emergencyResources != null && emergencyResources.isArray()) {
                    content.append("\nEmergency Resources:\n");
                    for (JsonNode resource : emergencyResources) {
                        content.append("- ").append(resource.get("name").asText())
                            .append(" (").append(resource.get("phone").asText()).append(")")
                            .append(": ").append(resource.get("description").asText()).append("\n");
                    }
                }

                // Include coping strategies
                JsonNode copingStrategies = root.get("copingStrategies");
                if (copingStrategies != null && copingStrategies.isArray()) {
                    content.append("\nCoping Strategies:\n");
                    for (JsonNode strategy : copingStrategies) {
                        content.append("- ").append(strategy.get("name").asText())
                            .append(": ").append(strategy.get("description").asText()).append("\n");
                    }
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "crisis-protocol");
                metadata.put("id", "safety-planning");
                metadata.put("name", "Safety Planning");

                Document doc = new Document("crisis-safety-planning", content.toString(), metadata);
                vectorStore.add(List.of(doc));
                count++;
            }

            log.info("Loaded {} crisis protocol documents", count);
        } catch (Exception e) {
            log.warn("Failed to load crisis protocols knowledge base: {}", e.getMessage());
        }
        return count;
    }

    private void appendSignList(StringBuilder content, String label, JsonNode signs) {
        if (signs != null && signs.isArray()) {
            content.append(label).append(":\n");
            for (JsonNode sign : signs) {
                content.append("- ").append(sign.asText()).append("\n");
            }
        }
    }
}
