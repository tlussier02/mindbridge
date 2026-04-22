package com.digitaltherapy.controller;

import com.digitaltherapy.config.JwtTokenProvider;
import com.digitaltherapy.dto.*;
import com.digitaltherapy.entity.User;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.DiaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiaryService diaryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    private ObjectMapper objectMapper;

    private static final UUID TEST_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_ENTRY_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

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

    // -------------------------------------------------------------- getEntries
    @Test
    void getEntries_Returns200() throws Exception {
        DiaryEntrySummary summary = DiaryEntrySummary.builder()
                .id(TEST_ENTRY_ID)
                .situation("Work presentation")
                .automaticThought("I will fail")
                .moodBefore(3)
                .moodAfter(6)
                .createdAt(LocalDateTime.of(2026, 2, 28, 9, 0, 0))
                .distortionCount(2)
                .build();

        Page<DiaryEntrySummary> page = new PageImpl<>(List.of(summary));

        when(diaryService.getEntries(any(UUID.class), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/diary/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TEST_ENTRY_ID.toString()))
                .andExpect(jsonPath("$.content[0].situation").value("Work presentation"))
                .andExpect(jsonPath("$.content[0].automaticThought").value("I will fail"))
                .andExpect(jsonPath("$.content[0].moodBefore").value(3))
                .andExpect(jsonPath("$.content[0].moodAfter").value(6))
                .andExpect(jsonPath("$.content[0].distortionCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ----------------------------------------------------------- createEntry
    @Test
    void createEntry_ValidRequest_Returns201() throws Exception {
        DiaryEntryCreate request = DiaryEntryCreate.builder()
                .situation("Meeting with manager")
                .automaticThought("He thinks I am incompetent")
                .emotions(List.of(
                        DiaryEntryCreate.EmotionRatingDto.builder()
                                .emotion("anxiety")
                                .intensity(8)
                                .build()
                ))
                .distortionIds(List.of("mind_reading"))
                .alternativeThought("I do not know what he thinks unless he tells me")
                .moodBefore(3)
                .moodAfter(6)
                .beliefRatingBefore(80)
                .beliefRatingAfter(40)
                .build();

        DiaryEntryResponse response = DiaryEntryResponse.builder()
                .id(TEST_ENTRY_ID)
                .situation("Meeting with manager")
                .automaticThought("He thinks I am incompetent")
                .emotions(List.of(
                        DiaryEntryResponse.EmotionRatingDto.builder()
                                .emotion("anxiety")
                                .intensity(8)
                                .build()
                ))
                .distortionIds(List.of("mind_reading"))
                .alternativeThought("I do not know what he thinks unless he tells me")
                .moodBefore(3)
                .moodAfter(6)
                .beliefRatingBefore(80)
                .beliefRatingAfter(40)
                .createdAt(LocalDateTime.of(2026, 2, 28, 14, 30, 0))
                .build();

        when(diaryService.createEntry(any(UUID.class), any(DiaryEntryCreate.class))).thenReturn(response);

        mockMvc.perform(post("/diary/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_ENTRY_ID.toString()))
                .andExpect(jsonPath("$.situation").value("Meeting with manager"))
                .andExpect(jsonPath("$.automaticThought").value("He thinks I am incompetent"))
                .andExpect(jsonPath("$.emotions[0].emotion").value("anxiety"))
                .andExpect(jsonPath("$.emotions[0].intensity").value(8))
                .andExpect(jsonPath("$.distortionIds[0]").value("mind_reading"))
                .andExpect(jsonPath("$.alternativeThought").value("I do not know what he thinks unless he tells me"))
                .andExpect(jsonPath("$.moodBefore").value(3))
                .andExpect(jsonPath("$.moodAfter").value(6))
                .andExpect(jsonPath("$.beliefRatingBefore").value(80))
                .andExpect(jsonPath("$.beliefRatingAfter").value(40));
    }

    @Test
    void createEntry_MissingSituation_Returns400() throws Exception {
        DiaryEntryCreate request = DiaryEntryCreate.builder()
                .automaticThought("Something bad will happen")
                .build();

        mockMvc.perform(post("/diary/entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------------- getEntryDetail
    @Test
    void getEntryDetail_Returns200() throws Exception {
        DiaryEntryDetail detail = DiaryEntryDetail.builder()
                .id(TEST_ENTRY_ID)
                .situation("Argument with friend")
                .automaticThought("Nobody likes me")
                .emotions(List.of(
                        DiaryEntryDetail.EmotionInfo.builder()
                                .emotion("sadness")
                                .intensity(7)
                                .build()
                ))
                .distortions(List.of(
                        DiaryEntryDetail.DistortionInfo.builder()
                                .id("overgeneralization")
                                .name("Overgeneralization")
                                .description("Drawing broad conclusions from a single event")
                                .build()
                ))
                .alternativeThought("One disagreement does not mean nobody likes me")
                .moodBefore(2)
                .moodAfter(5)
                .beliefRatingBefore(90)
                .beliefRatingAfter(30)
                .createdAt(LocalDateTime.of(2026, 2, 27, 18, 0, 0))
                .build();

        when(diaryService.getEntryDetail(any(UUID.class))).thenReturn(detail);

        mockMvc.perform(get("/diary/entries/{entryId}", TEST_ENTRY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_ENTRY_ID.toString()))
                .andExpect(jsonPath("$.situation").value("Argument with friend"))
                .andExpect(jsonPath("$.automaticThought").value("Nobody likes me"))
                .andExpect(jsonPath("$.emotions[0].emotion").value("sadness"))
                .andExpect(jsonPath("$.emotions[0].intensity").value(7))
                .andExpect(jsonPath("$.distortions[0].id").value("overgeneralization"))
                .andExpect(jsonPath("$.distortions[0].name").value("Overgeneralization"))
                .andExpect(jsonPath("$.alternativeThought").value("One disagreement does not mean nobody likes me"))
                .andExpect(jsonPath("$.moodBefore").value(2))
                .andExpect(jsonPath("$.moodAfter").value(5));
    }

    // ----------------------------------------------------------- deleteEntry
    @Test
    void deleteEntry_Returns204() throws Exception {
        doNothing().when(diaryService).deleteEntry(any(UUID.class));

        mockMvc.perform(delete("/diary/entries/{entryId}", TEST_ENTRY_ID))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------- suggestDistortions
    @Test
    void suggestDistortions_Returns200() throws Exception {
        DistortionSuggestion suggestion = DistortionSuggestion.builder()
                .distortionId("catastrophizing")
                .name("Catastrophizing")
                .confidence(0.85)
                .reasoning("The thought assumes the worst possible outcome without evidence.")
                .build();

        when(diaryService.suggestDistortions(anyString())).thenReturn(List.of(suggestion));

        Map<String, String> body = Map.of("thought", "Everything is going to go wrong");

        mockMvc.perform(post("/diary/distortions/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distortionId").value("catastrophizing"))
                .andExpect(jsonPath("$[0].name").value("Catastrophizing"))
                .andExpect(jsonPath("$[0].confidence").value(0.85))
                .andExpect(jsonPath("$[0].reasoning").value("The thought assumes the worst possible outcome without evidence."));
    }

    // ------------------------------------------------------------ getInsights
    @Test
    void getInsights_Returns200() throws Exception {
        DiaryInsights insights = DiaryInsights.builder()
                .totalEntries(15)
                .averageMoodImprovement(2.3)
                .topDistortions(List.of(
                        DiaryInsights.DistortionFrequency.builder()
                                .distortionId("catastrophizing")
                                .name("Catastrophizing")
                                .count(7)
                                .build()
                ))
                .patterns(List.of("Negative thoughts peak on Monday mornings"))
                .recommendations(List.of("Try a short breathing exercise before work"))
                .build();

        when(diaryService.getInsights(any(UUID.class))).thenReturn(insights);

        mockMvc.perform(get("/diary/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(15))
                .andExpect(jsonPath("$.averageMoodImprovement").value(2.3))
                .andExpect(jsonPath("$.topDistortions[0].distortionId").value("catastrophizing"))
                .andExpect(jsonPath("$.topDistortions[0].name").value("Catastrophizing"))
                .andExpect(jsonPath("$.topDistortions[0].count").value(7))
                .andExpect(jsonPath("$.patterns[0]").value("Negative thoughts peak on Monday mornings"))
                .andExpect(jsonPath("$.recommendations[0]").value("Try a short breathing exercise before work"));
    }
}
