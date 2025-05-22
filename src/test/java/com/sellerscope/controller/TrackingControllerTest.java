package com.sellerscope.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.service.WbProductParserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.Mockito.when;

@WebMvcTest(TrackingController.class)
class TrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WbProductParserService service;

    @MockitoBean
    private ProductSnapshotRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnHistoryForArticle() throws Exception {
        ProductSnapshot snapshot = ProductSnapshot.builder()
                .productId("123")
                .name("Example product")
                .price(BigDecimal.valueOf(100))
                .reviewCount(10)
                .rating(4.5)
                .photoHash("a")
                .descriptionHash("b")
                .createdAt(LocalDateTime.now())
                .changed(false)
                .build();

        when(service.getSnapshotHistory("123")).thenReturn(List.of(snapshot));

        mockMvc.perform(get("/track/history/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("123"))
                .andExpect(jsonPath("$[0].name").value("Example product"));
    }

    @Test
    void shouldTrackNewSnapshot() throws Exception {
        ProductSnapshot snapshot = ProductSnapshot.builder()
                .productId("321")
                .name("Added product")
                .price(BigDecimal.valueOf(200))
                .reviewCount(5)
                .rating(4.2)
                .photoHash("x")
                .descriptionHash("y")
                .createdAt(LocalDateTime.now())
                .changed(false)
                .build();

        when(service.fetchSnapshotByArticle("321")).thenReturn(snapshot);
        when(repository.save(snapshot)).thenReturn(snapshot);

        mockMvc.perform(post("/track/321"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("321"))
                .andExpect(jsonPath("$.name").value("Added product"));
    }

    @Test
    void shouldReturnBadRequestOnInvalidArticle() throws Exception {
        when(service.fetchSnapshotByArticle("bad")).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(post("/track/bad"))
                .andExpect(status().isBadRequest());
    }
}