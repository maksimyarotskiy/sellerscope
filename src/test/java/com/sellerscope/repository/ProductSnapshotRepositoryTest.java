package com.sellerscope.repository;

import com.sellerscope.entity.ProductSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductSnapshotRepositoryTest {

    @Autowired
    private ProductSnapshotRepository repository;

    @Test
    @DisplayName("Должен находить снапшоты по productId в порядке убывания даты")
    void shouldFindSnapshotsByProductIdInDescendingOrder() {
        String productId = "test-123";

        ProductSnapshot older = ProductSnapshot.builder()
                .productId(productId)
                .name("Old")
                .price(BigDecimal.valueOf(100))
                .reviewCount(10)
                .rating(4.5)
                .photoHash("old")
                .descriptionHash("old")
                .createdAt(LocalDateTime.now().minusDays(1))
                .changed(false)
                .build();

        ProductSnapshot newer = ProductSnapshot.builder()
                .productId(productId)
                .name("New")
                .price(BigDecimal.valueOf(150))
                .reviewCount(20)
                .rating(4.8)
                .photoHash("new")
                .descriptionHash("new")
                .createdAt(LocalDateTime.now())
                .changed(true)
                .build();

        repository.save(older);
        repository.save(newer);

        List<ProductSnapshot> result = repository.findByProductIdOrderByCreatedAtDesc(productId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("New");
        assertThat(result.get(1).getName()).isEqualTo("Old");
    }
}