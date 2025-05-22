package com.sellerscope.controller;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.service.WbProductParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/history/{article}")
    public ResponseEntity<List<ProductSnapshot>> getHistory(@PathVariable String article) {
        List<ProductSnapshot> history = wbService.getSnapshotHistory(article);
        return ResponseEntity.ok(history);
    }
}