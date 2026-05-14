package com.digitaltherapy.controller;

import com.digitaltherapy.config.JwtTokenProvider;
import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private ObjectMapper objectMapper;

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEST_USER_SESSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        User mockUser = User.builder().id(TEST_USER_ID).name("Test User").email("test@example.com").build();
        var auth = new UsernamePasswordAuthenticationToken(mockUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------- getSessionLibrary
    @Test
    void getSessions_Returns200() throws Exception {
        SessionModuleDto module = SessionModuleDto.builder()
                .id(UUID.randomUUID())
                .name("Core CBT Module")
                .description("Foundational CBT techniques")
                .category("CORE")
                .orderIndex(1)
                .sessions(List.of(
                        SessionModuleDto.SessionSummaryItem.builder()
                                .id(TEST_SESSION_ID)
                                .title("Identifying Negative Thoughts")
                                .durationMinutes(30)
                                .build()
                ))
                .build();

        when(sessionService.getSessionLibrary(any(UUID.class))).thenReturn(List.of(module));

        mockMvc.perform(get("/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Core CBT Module"))
                .andExpect(jsonPath("$[0].category").value("CORE"))
                .andExpect(jsonPath("$[0].orderIndex").value(1))
                .andExpect(jsonPath("$[0].sessions[0].title").value("Identifying Negative Thoughts"))
                .andExpect(jsonPath("$[0].sessions[0].durationMinutes").value(30));
    }

    // -------------------------------------------------------- getSessionDetails
    @Test
    void getSessionDetails_Returns200() throws Exception {
        SessionDetail detail = SessionDetail.builder()
                .id(TEST_SESSION_ID)
                .title("Cognitive Restructuring")
                .description("Learn to identify and challenge unhelpful thoughts")
                .durationMinutes(45)
                .objectives(List.of("Identify automatic thoughts", "Challenge distortions"))
                .modalities(List.of("TEXT", "EXERCISE"))
                .moduleName("Core CBT Module")
                .build();

        when(sessionService.getSessionDetails(any(UUID.class))).thenReturn(detail);

        mockMvc.perform(get("/sessions/{sessionId}", TEST_SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_SESSION_ID.toString()))
                .andExpect(jsonPath("$.title").value("Cognitive Restructuring"))
                .andExpect(jsonPath("$.description").value("Learn to identify and challenge unhelpful thoughts"))
                .andExpect(jsonPath("$.durationMinutes").value(45))
                .andExpect(jsonPath("$.objectives[0]").value("Identify automatic thoughts"))
                .andExpect(jsonPath("$.modalities[1]").value("EXERCISE"))
                .andExpect(jsonPath("$.moduleName").value("Core CBT Module"));
    }

    // ----------------------------------------------------------- startSession
    @Test
    void startSession_Returns201() throws Exception {
        ActiveSession activeSession = ActiveSession.builder()
                .sessionId(TEST_SESSION_ID)
                .userSessionId(TEST_USER_SESSION_ID)
                .title("Cognitive Restructuring")
                .description("Learn to identify and challenge unhelpful thoughts")
                .startedAt(LocalDateTime.of(2026, 2, 28, 10, 0, 0))
                .moodBefore(5)
                .build();

        when(sessionService.startSession(any(UUID.class), any(UUID.class)))
                .thenReturn(activeSession);

        Map<String, Object> body = Map.of("moodBefore", 5);

        mockMvc.perform(post("/sessions/{sessionId}/start", TEST_SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(TEST_SESSION_ID.toString()))
                .andExpect(jsonPath("$.userSessionId").value(TEST_USER_SESSION_ID.toString()))
                .andExpect(jsonPath("$.title").value("Cognitive Restructuring"))
                .andExpect(jsonPath("$.moodBefore").value(5));
    }

    // ------------------------------------------------------------------- chat
    @Test
    void chat_ValidMessage_Returns200() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("I feel anxious about my presentation tomorrow")
                .modality("TEXT")
                .build();

        ChatResponse response = ChatResponse.builder()
                .message("It sounds like you are experiencing anticipatory anxiety. Let us explore that together.")
                .role("ASSISTANT")
                .timestamp(LocalDateTime.of(2026, 2, 28, 10, 5, 0))
                .crisisDetected(false)
                .crisisAction(null)
                .build();

        when(sessionService.chat(any(UUID.class), any(UUID.class), any(String.class))).thenReturn(response);

        mockMvc.perform(post("/sessions/{sessionId}/chat", TEST_SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("It sounds like you are experiencing anticipatory anxiety. Let us explore that together."))
                .andExpect(jsonPath("$.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.crisisDetected").value(false));
    }

    @Test
    void chat_BlankMessage_Returns400() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("")
                .modality("TEXT")
                .build();

        mockMvc.perform(post("/sessions/{sessionId}/chat", TEST_SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------------------- endSession
    @Test
    void endSession_Returns200() throws Exception {
        SessionSummary summary = SessionSummary.builder()
                .sessionId(TEST_USER_SESSION_ID)
                .title("Cognitive Restructuring")
                .status("COMPLETED")
                .startedAt(LocalDateTime.of(2026, 2, 28, 10, 0, 0))
                .endedAt(LocalDateTime.of(2026, 2, 28, 10, 45, 0))
                .moodBefore(4)
                .moodAfter(7)
                .summary("Session completed.")
                .keyInsights(List.of("Identified catastrophizing pattern"))
                .build();

        when(sessionService.endSession(any(UUID.class), any(UUID.class), any())).thenReturn(summary);

        Map<String, Object> body = Map.of("reason", "completed");

        mockMvc.perform(post("/sessions/{sessionId}/end", TEST_SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(TEST_USER_SESSION_ID.toString()))
                .andExpect(jsonPath("$.title").value("Cognitive Restructuring"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.moodBefore").value(4))
                .andExpect(jsonPath("$.moodAfter").value(7))
                .andExpect(jsonPath("$.summary").value("Session completed."))
                .andExpect(jsonPath("$.keyInsights[0]").value("Identified catastrophizing pattern"));
    }
}
