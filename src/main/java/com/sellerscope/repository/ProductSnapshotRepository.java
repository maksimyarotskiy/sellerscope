package com.sellerscope.repository;

import com.sellerscope.entity.ProductSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductSnapshotRepository extends JpaRepository<ProductSnapshot, Long> {
    List<ProductSnapshot> findByProductIdOrderByCreatedAtDesc(String productId);
}