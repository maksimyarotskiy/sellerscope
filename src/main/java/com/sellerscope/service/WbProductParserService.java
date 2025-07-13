package com.sellerscope.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerscope.config.RedisConfig;
import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class WbProductParserService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ProductSnapshotRepository repository;
    private final RedissonClient redissonClient;

    public WbProductParserService(ProductSnapshotRepository repository,
                                  RedissonClient redissonClient) {
        this.repository = repository;
        this.redissonClient = redissonClient;
    }

    public ProductSnapshot fetchSnapshotByArticle(String article) {
        String url = "https://card.wb.ru/cards/detail?appType=1&curr=rub&dest=-1257786&spp=0&nm=" + article;
        RLock lock = redissonClient.getLock("lock:product:" + article);
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
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

                    JsonNode photosNode = product.get("photos");
                    String photosCombined = "";

                    if (photosNode != null && photosNode.isArray()) {
                        photosCombined = StreamSupport.stream(photosNode.spliterator(), false)
                                .map(JsonNode::asText)
                                .collect(Collectors.joining(","));
                    }

                    String photoHash = DigestUtils.md5DigestAsHex(photosCombined.getBytes(StandardCharsets.UTF_8));

                    String description = product.has("description") ? product.get("description").asText() : "";
                    String descriptionHash = DigestUtils.md5DigestAsHex(description.getBytes(StandardCharsets.UTF_8));

                    BigDecimal price = new BigDecimal(product.get("priceU").asText()).divide(BigDecimal.valueOf(100)); // priceU — в копейках

                    ProductSnapshot snapshot = ProductSnapshot.builder()
                            .productId(article)
                            .name(name)
                            .price(price)
                            .reviewCount(reviewCount)
                            .rating(rating)
                            .photoHash(photoHash)
                            .descriptionHash(descriptionHash)
                            .createdAt(LocalDateTime.now())
                            .changedFields(new HashSet<>())
                            .build();

                    boolean changed = compareWithLastSnapshot(snapshot);
                    snapshot.setChanged(changed);

                    return snapshot;

                } catch (Exception e) {
                    throw new RuntimeException("Ошибка при получении товара: " + e.getMessage());
                }
            } else {
                throw new RuntimeException("Пропущено обновление: товар сейчас обновляется другим потоком.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while locking product");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    public boolean compareWithLastSnapshot(ProductSnapshot currentSnapshot) {
        return repository.findByProductIdOrderByCreatedAtDesc(currentSnapshot.getProductId())
                .stream()
                .findFirst()
                .map(last -> {
                    boolean changed = false;

                    if (last.getPrice().compareTo(currentSnapshot.getPrice()) != 0) {
                        currentSnapshot.getChangedFields().add("price");
                        changed = true;
                    }
                    if (last.getReviewCount() != currentSnapshot.getReviewCount()) {
                        currentSnapshot.getChangedFields().add("reviewCount");
                        changed = true;
                    }
                    if (Double.compare(last.getRating(), currentSnapshot.getRating()) != 0) {
                        currentSnapshot.getChangedFields().add("rating");
                        changed = true;
                    }
                    if (!Objects.equals(last.getPhotoHash(), currentSnapshot.getPhotoHash())) {
                        currentSnapshot.getChangedFields().add("photos");
                        changed = true;
                    }
                    if (!Objects.equals(last.getDescriptionHash(), currentSnapshot.getDescriptionHash())) {
                        currentSnapshot.getChangedFields().add("description");
                        changed = true;
                    }

                    currentSnapshot.setChanged(changed);
                    return changed;
                })
                .orElseGet(() -> {
                    currentSnapshot.getChangedFields().add("new");
                    currentSnapshot.setChanged(true);
                    return true;
                });
    }

    public List<ProductSnapshot> getSnapshotHistory(String article) {
        return repository.findByProductIdOrderByCreatedAtDesc(article);
    }
}