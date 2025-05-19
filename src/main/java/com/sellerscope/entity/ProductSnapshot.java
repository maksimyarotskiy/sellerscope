package com.sellerscope.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;

    private String name;
    private BigDecimal price;
    private int reviewCount;
    private double rating;

    private String photoHash;
    private String descriptionHash;

    private LocalDateTime createdAt;
    private boolean changed;
}