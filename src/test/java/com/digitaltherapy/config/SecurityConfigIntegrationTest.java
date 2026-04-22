package com.digitaltherapy.config;

import com.digitaltherapy.controller.AuthController;
import com.digitaltherapy.controller.CrisisController;
import com.digitaltherapy.controller.DiaryController;
import com.digitaltherapy.controller.SessionController;
import com.digitaltherapy.repository.UserRepository;
import com.digitaltherapy.service.AuthService;
import com.digitaltherapy.service.CrisisService;
import com.digitaltherapy.service.DiaryService;
import com.digitaltherapy.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuthController.class, CrisisController.class, SessionController.class, DiaryController.class})
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CrisisService crisisService;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private DiaryService diaryService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Auth login endpoint is accessible without authentication")
    void authLogin_NoAuth_IsAccessible() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"test@test.com\",\"password\":\"password\"}"))
                .andExpect(status().is4xxClientError()); // 400 due to validation, not 401/403
    }

    @Test
    @DisplayName("Auth register endpoint is accessible without authentication")
    void authRegister_NoAuth_IsAccessible() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"test@test.com\",\"password\":\"password\",\"name\":\"Test\"}"))
                .andExpect(status().is4xxClientError()); // validation error, not 401
    }

    @Test
    @DisplayName("Crisis detect endpoint does not return 401 (publicly accessible)")
    void crisisDetect_NoAuth_NotUnauthorized() throws Exception {
        mockMvc.perform(post("/crisis/detect")
                        .contentType("application/json")
                        .content("{\"text\":\"I am feeling stressed\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401));
    }

    @Test
    @DisplayName("Protected session endpoint returns 401 without auth token")
    void sessionLibrary_NoAuth_Returns401() throws Exception {
        mockMvc.perform(get("/sessions/library"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected diary endpoint returns 401 without auth token")
    void diaryEntries_NoAuth_Returns401() throws Exception {
        mockMvc.perform(get("/diary/entries"))
                .andExpect(status().isUnauthorized());
    }
}
