package com.sellerscope.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class WbProductParserService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProductSnapshotRepository repository;

    public WbProductParserService(ProductSnapshotRepository repository) {
        this.repository = repository;
    }

    public ProductSnapshot fetchSnapshotByArticle(String article) {
        String url = "https://card.wb.ru/cards/detail?appType=1&curr=rub&dest=-1257786&spp=0&nm=" + article;

        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);

            JsonNode product = root.at("/data/products/0");
            if (product.isMissingNode()) {
                throw new RuntimeException("Product not found");
            }

            String name = product.get("name").asText();
            int reviewCount = product.get("feedbacks").asInt();
            double rating = product.get("rating").asDouble();

            BigDecimal price = new BigDecimal(product.get("priceU").asText()).divide(BigDecimal.valueOf(100)); // priceU — в копейках

            // TODO: потом добавлю расчёт hash'ей по фото и описанию
            ProductSnapshot snapshot = ProductSnapshot.builder()
                    .productId(article)
                    .name(name)
                    .price(price)
                    .reviewCount(reviewCount)
                    .rating(rating)
                    .photoHash("stub-photo")
                    .descriptionHash("stub-description")
                    .createdAt(LocalDateTime.now())
                    .build();

            boolean changed = compareWithLastSnapshot(snapshot);
            snapshot.setChanged(changed);

            return snapshot;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при получении товара: " + e.getMessage());
        }
    }

    public boolean compareWithLastSnapshot(ProductSnapshot currentSnapshot) {
        return repository.findByProductIdOrderByCreatedAtDesc(currentSnapshot.getProductId())
                .stream()
                .findFirst()
                .map(last -> {
                    boolean changed =
                            last.getPrice().compareTo(currentSnapshot.getPrice()) != 0 ||
                                    last.getReviewCount() != currentSnapshot.getReviewCount() ||
                                    Double.compare(last.getRating(), currentSnapshot.getRating()) != 0 ||
                                    !Objects.equals(last.getPhotoHash(), currentSnapshot.getPhotoHash()) ||
                                    !Objects.equals(last.getDescriptionHash(), currentSnapshot.getDescriptionHash());
                    return changed;
                }).orElse(true);
    }
}