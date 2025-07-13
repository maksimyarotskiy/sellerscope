package com.sellerscope.repository;

import com.sellerscope.entity.TrackedProduct;
import com.sellerscope.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrackedProductRepository extends JpaRepository<TrackedProduct, Long> {

    @Query("SELECT DISTINCT t.productId FROM TrackedProduct t")
    List<String> findAllDistinctProductIds();
    boolean existsByUserAndProductId(User user, String productId);
}
