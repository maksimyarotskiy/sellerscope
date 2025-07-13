package com.sellerscope.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "snapshot_changes", joinColumns = @JoinColumn(name = "snapshot_id"))
    @Column(name = "field")
    private Set<String> changedFields = new HashSet<>();
}