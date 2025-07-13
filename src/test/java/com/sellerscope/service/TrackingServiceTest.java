package com.sellerscope.service;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.entity.TrackedProduct;
import com.sellerscope.entity.User;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.repository.TrackedProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrackingServiceTest {

    private WbProductParserService wbService;
    private ProductSnapshotRepository snapshotRepository;
    private TrackedProductRepository trackedProductRepository;
    private TrackingService trackingService;

    @BeforeEach
    void setUp() {
        wbService = mock(WbProductParserService.class);
        snapshotRepository = mock(ProductSnapshotRepository.class);
        trackedProductRepository = mock(TrackedProductRepository.class);
        trackingService = new TrackingService(wbService, snapshotRepository, trackedProductRepository);
    }

    @Test
    void shouldTrackProductSuccessfully() {
        User user = User.builder().id(1L).email("test@mail.com").build();
        String article = "123";
        ProductSnapshot snapshot = ProductSnapshot.builder().productId(article).build();

        when(trackedProductRepository.existsByUserAndProductId(user, article)).thenReturn(false);
        when(wbService.fetchSnapshotByArticle(article)).thenReturn(snapshot);

        ProductSnapshot result = trackingService.trackProduct(user, article);

        assertThat(result).isEqualTo(snapshot);
        verify(snapshotRepository).save(snapshot);
        verify(trackedProductRepository).save(any(TrackedProduct.class));
    }

    @Test
    void shouldThrowExceptionIfAlreadyTracked() {
        User user = User.builder().id(1L).email("test@mail.com").build();
        String article = "123";

        when(trackedProductRepository.existsByUserAndProductId(user, article)).thenReturn(true);

        assertThatThrownBy(() -> trackingService.trackProduct(user, article))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User already tracking product");
        verifyNoInteractions(wbService);
        verifyNoInteractions(snapshotRepository);
    }
}