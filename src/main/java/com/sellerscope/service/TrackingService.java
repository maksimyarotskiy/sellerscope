package com.sellerscope.service;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.entity.TrackedProduct;
import com.sellerscope.entity.User;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.repository.TrackedProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Сервис для управления отслеживанием товаров пользователями.
 * Вся бизнес-логика по добавлению товара в отслеживание вынесена сюда.
 */
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final WbProductParserService wbService;
    private final ProductSnapshotRepository repository;
    private final TrackedProductRepository trackedProductRepository;

    /**
     * Добавляет товар в отслеживание для пользователя.
     * Если пользователь уже отслеживает товар, выбрасывает исключение.
     *
     * @param user    пользователь, который хочет отслеживать товар
     * @param article идентификатор товара (артикул)
     * @return созданный и сохранённый снапшот товара
     * @throws IllegalStateException если пользователь уже отслеживает этот товар
     */
    public ProductSnapshot trackProduct(User user, String article) {
        // Проверка: если пользователь уже отслеживает этот товар — выбрасываем исключение
        if (trackedProductRepository.existsByUserAndProductId(user, article)) {
            throw new IllegalStateException("User already tracking product: " + article);
        }

        // Получаем актуальный снапшот товара через парсер
        ProductSnapshot snapshot = wbService.fetchSnapshotByArticle(article);

        // Сохраняем снапшот в базу данных
        repository.save(snapshot);

        // Сохраняем информацию о том, что пользователь начал отслеживать этот товар
        trackedProductRepository.save(
                TrackedProduct.builder()
                        .user(user)
                        .productId(article)
                        .trackedSince(LocalDateTime.now())
                        .build()
        );

        // Возвращаем снапшот для ответа контроллеру
        return snapshot;
    }
}