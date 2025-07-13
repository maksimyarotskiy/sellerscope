package com.sellerscope.service;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WbProductParserServiceTest {

    private ProductSnapshotRepository repository;
    private RedissonClient redissonClient;
    private WbProductParserService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductSnapshotRepository.class);
        redissonClient = mock(RedissonClient.class);
        service = new WbProductParserService(repository, redissonClient);
    }

    @Test
    void shouldReturnTrueWhenSnapshotIsDifferent() {
        ProductSnapshot last = ProductSnapshot.builder()
                .productId("123")
                .name("Old")
                .price(BigDecimal.valueOf(100))
                .reviewCount(10)
                .rating(4.5)
                .photoHash("abc")
                .descriptionHash("xyz")
                .changedFields(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        ProductSnapshot current = ProductSnapshot.builder()
                .productId("123")
                .name("Old")
                .price(BigDecimal.valueOf(200)) // 👈 изменилось
                .reviewCount(10)
                .rating(4.5)
                .photoHash("abc")
                .descriptionHash("xyz")
                .changedFields(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByProductIdOrderByCreatedAtDesc("123"))
                .thenReturn(List.of(last));

        boolean changed = service.compareWithLastSnapshot(current);

        assertThat(changed).isTrue();
        assertThat(current.getChangedFields()).containsExactly("price");
    }

    @Test
    void shouldReturnFalseWhenSnapshotIsSame() {
        ProductSnapshot snap = ProductSnapshot.builder()
                .productId("456")
                .name("Same")
                .price(BigDecimal.valueOf(150))
                .reviewCount(5)
                .rating(5.0)
                .photoHash("x")
                .descriptionHash("y")
                .changedFields(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByProductIdOrderByCreatedAtDesc("456"))
                .thenReturn(List.of(snap));

        boolean changed = service.compareWithLastSnapshot(snap);
        assertThat(changed).isFalse();
        assertThat(snap.getChangedFields()).isEmpty();
    }

    @Test
    void shouldReturnTrueWhenNoPreviousSnapshots() {
        ProductSnapshot current = ProductSnapshot.builder()
                .productId("789")
                .name("New")
                .price(BigDecimal.valueOf(99))
                .reviewCount(0)
                .rating(0.0)
                .photoHash("a")
                .descriptionHash("b")
                .changedFields(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findByProductIdOrderByCreatedAtDesc("789"))
                .thenReturn(List.of());

        boolean changed = service.compareWithLastSnapshot(current);
        assertThat(changed).isTrue();
        assertThat(current.getChangedFields()).isNotEmpty();
    }
}