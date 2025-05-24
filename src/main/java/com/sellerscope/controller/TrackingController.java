package com.sellerscope.controller;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.service.WbProductParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/track")
public class TrackingController {

    private final WbProductParserService wbService;
    private final ProductSnapshotRepository repository;

    public TrackingController(WbProductParserService wbService, ProductSnapshotRepository repository) {
        this.wbService = wbService;
        this.repository = repository;
    }

    // POST /track/{article} — парсит карточку WB и сохраняет снапшот
    @PostMapping("/{article}")
    public ResponseEntity<ProductSnapshot> trackProduct(@PathVariable String article) {
        try {
            ProductSnapshot snapshot = wbService.fetchSnapshotByArticle(article);
            ProductSnapshot saved = repository.save(snapshot);
            return ResponseEntity.ok(saved);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        }
    }

    // GET /track/history/{article} — получает историю сохранения снепшотов(даже те, которые не изменились)
    @GetMapping("/history/{article}")
    public ResponseEntity<List<ProductSnapshot>> getHistory(@PathVariable String article) {
        List<ProductSnapshot> history = wbService.getSnapshotHistory(article);
        return ResponseEntity.ok(history);
    }

    // GET /track/changes/{article} — получишь снапшоты, где были изменения
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