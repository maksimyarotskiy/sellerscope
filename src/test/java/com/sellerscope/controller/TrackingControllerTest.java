package com.sellerscope.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerscope.config.TestSecurityConfig;
import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.entity.User;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.service.TrackingService;
import com.sellerscope.service.WbProductParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrackingController.class)
@Import(TestSecurityConfig.class) // ВАЖНО: импортируйте тестовую конфигурацию!
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.sellerscope.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.sellerscope.service.JwtService jwtService;

    @MockBean
    private WbProductParserService wbService;

    @MockBean
    private ProductSnapshotRepository repository;

    @MockBean
    private TrackingService trackingService;

    private User user;
    private ProductSnapshot snapshot;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .role(null)
                .build();

        snapshot = ProductSnapshot.builder()
                .id(1L)
                .productId("12345")
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .reviewCount(100)
                .rating(4.5)
                .photoHash("photoHash123")
                .descriptionHash("descHash123")
                .createdAt(LocalDateTime.now())
                .changed(true)
                .changedFields(Set.of("price", "rating"))
                .build();

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void trackProduct_Success_ReturnsOkWithSnapshot() throws Exception {
        when(trackingService.trackProduct(any(User.class), eq("12345"))).thenReturn(snapshot);

        mockMvc.perform(post("/track/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.productId").value("12345"))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99))
                .andExpect(jsonPath("$.reviewCount").value(100))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.photoHash").value("photoHash123"))
                .andExpect(jsonPath("$.descriptionHash").value("descHash123"))
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.changedFields").value(List.of("price", "rating")));
    }

    @Test
    void trackProduct_IllegalStateException_ReturnsConflict() throws Exception {
        when(trackingService.trackProduct(any(User.class), eq("12345")))
                .thenThrow(new IllegalStateException("User already tracking product: 12345"));

        mockMvc.perform(post("/track/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void trackProduct_RuntimeException_ReturnsInternalServerError() throws Exception {
        when(trackingService.trackProduct(any(User.class), eq("12345")))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/track/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getHistory_Success_ReturnsSnapshotList() throws Exception {
        List<ProductSnapshot> history = List.of(snapshot);
        when(wbService.getSnapshotHistory("12345")).thenReturn(history);

        mockMvc.perform(get("/track/history/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].productId").value("12345"))
                .andExpect(jsonPath("$[0].name").value("Test Product"))
                .andExpect(jsonPath("$[0].price").value(99.99))
                .andExpect(jsonPath("$[0].changedFields").value(List.of("price", "rating")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getChanges_Success_ReturnsFilteredSnapshotList() throws Exception {
        ProductSnapshot unchangedSnapshot = ProductSnapshot.builder()
                .id(2L)
                .productId("12345")
                .name("Test Product")
                .price(new BigDecimal("99.99"))
                .reviewCount(100)
                .rating(4.5)
                .photoHash("photoHash123")
                .descriptionHash("descHash123")
                .createdAt(LocalDateTime.now())
                .changed(false)
                .changedFields(Set.of())
                .build();

        List<ProductSnapshot> snapshots = List.of(snapshot, unchangedSnapshot);
        when(repository.findByProductIdOrderByCreatedAtDesc("12345")).thenReturn(snapshots);

        mockMvc.perform(get("/track/changes/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].productId").value("12345"))
                .andExpect(jsonPath("$[0].changed").value(true))
                .andExpect(jsonPath("$[0].changedFields").value(List.of("price", "rating")))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getChangedFields_Success_ReturnsChangedFieldsList() throws Exception {
        List<ProductSnapshot> snapshots = List.of(snapshot);
        when(repository.findByProductIdOrderByCreatedAtDesc("12345")).thenReturn(snapshots);

        mockMvc.perform(get("/track/changed-fields/12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].changedFields").value(List.of("price", "rating")))
                .andExpect(jsonPath("$.length()").value(1));
    }
}