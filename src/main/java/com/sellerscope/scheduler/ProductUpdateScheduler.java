package com.sellerscope.scheduler;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.repository.TrackedProductRepository;
import com.sellerscope.service.WbProductParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Планировщик для периодического обновления информации о товарах, которые отслеживают пользователи.
 * <p>
 * Каждые 10 минут:
 * <ul>
 *     <li>Получает список уникальных артикулов отслеживаемых товаров.</li>
 *     <li>Для каждого артикула получает Redis-лок для предотвращения параллельных обновлений.</li>
 *     <li>Обновляет снапшот товара через сервис парсинга.</li>
 *     <li>Сохраняет изменённые снапшоты в базу данных.</li>
 *     <li>Логирует информацию об обновлениях.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUpdateScheduler {

    private final ProductSnapshotRepository productSnapshotRepository;
    private final TrackedProductRepository trackedProductRepository;
    private final WbProductParserService wbProductParserService;
    private final RedissonClient redissonClient;

    /**
     * Запускается каждые 10 минут.
     * Для каждого уникального артикула товара:
     * <ul>
     *     <li>Получает Redis-лок.</li>
     *     <li>Обновляет снапшот товара через сервис.</li>
     *     <li>Сохраняет изменённые снапшоты.</li>
     *     <li>Логирует изменения.</li>
     * </ul>
     */
    @Scheduled(fixedRate = 600_000)
    public void updateTrackedProducts() {
        log.info("Запущено обновление отслеживаемых товаров...");

        List<String> distinctArticles = trackedProductRepository.findAllDistinctProductIds();
        List<ProductSnapshot> updatedSnapshots = new ArrayList<>();

        for (String article : distinctArticles) {
            String lockKey = "product-update-lock:" + article;
            RLock lock = redissonClient.getLock(lockKey);
            boolean locked = false;
            try {
                locked = lock.tryLock();
                if (locked) {
                    ProductSnapshot snapshot = wbProductParserService.fetchSnapshotByArticle(article);
                    if (snapshot != null && snapshot.isChanged()) {
                        productSnapshotRepository.save(snapshot);
                        updatedSnapshots.add(snapshot);
                        log.info("Товар {} обновлён, изменённые поля: {}", article, snapshot.getChangedFields());
                    } else {
                        log.debug("Товар {} не изменился", article);
                    }
                } else {
                    log.warn("Не удалось получить лок для товара {}, пропускаем обновление", article);
                }
            } catch (Exception e) {
                log.warn("Ошибка обновления товара {}: {}", article, e.getMessage());
            } finally {
                if (locked) {
                    lock.unlock();
                }
            }
        }

        log.info("Обновлено {} товаров", updatedSnapshots.size());
    }
}