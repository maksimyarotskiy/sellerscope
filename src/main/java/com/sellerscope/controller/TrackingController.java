package com.sellerscope.controller;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.entity.TrackedProduct;
import com.sellerscope.entity.User;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.repository.TrackedProductRepository;
import com.sellerscope.service.TrackingService;
import com.sellerscope.service.WbProductParserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/track")
public class TrackingController {

    private final WbProductParserService wbService;
    private final ProductSnapshotRepository repository;
    private final TrackingService trackingService;

    public TrackingController(
            WbProductParserService wbService,
            ProductSnapshotRepository repository,
            TrackingService trackingService
    ) {
        this.wbService = wbService;
        this.repository = repository;
        this.trackingService = trackingService;
    }

    // POST /track/{article} — отслеживает товар по артикулу
    @PostMapping("/{article}")
    public ResponseEntity<ProductSnapshot> trackProduct(@PathVariable String article) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getPrincipal() instanceof User user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            ProductSnapshot snapshot = trackingService.trackProduct(user, article);
            return ResponseEntity.ok(snapshot);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // GET /track/history/{article} — получает историю сохранения снепшотов(даже те, которые не изменились)
    @GetMapping("/history/{article}")
    public ResponseEntity<List<ProductSnapshot>> getHistory(@PathVariable String article) {
        List<ProductSnapshot> history = wbService.getSnapshotHistory(article);
        return ResponseEntity.ok(history);
    }

    // GET /track/changes/{article} — получает снепшоты, где были изменения продукта
    @GetMapping("/changes/{article}")
    public ResponseEntity<List<ProductSnapshot>> getChanges(@PathVariable String article) {
        List<ProductSnapshot> changes = repository.findByProductIdOrderByCreatedAtDesc(article).stream()
                .filter(ProductSnapshot::isChanged)
                .toList();

        return ResponseEntity.ok(changes);
    }

    // GET /track/changed-fields/{article} - получает только измененные поля в карточке, где были изменения
    @GetMapping("/changed-fields/{article}")
    public ResponseEntity<List<Map<String, Object>>> getChangedFields(@PathVariable String article) {
        List<Map<String, Object>> result = repository.findByProductIdOrderByCreatedAtDesc(article).stream()
                .filter(ProductSnapshot::isChanged)
                .map(snapshot -> Map.of(
                        "createdAt", snapshot.getCreatedAt(),
                        "changedFields", snapshot.getChangedFields()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }
}