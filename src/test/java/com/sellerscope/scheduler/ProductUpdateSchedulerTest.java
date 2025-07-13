package com.sellerscope.scheduler;

import com.sellerscope.entity.ProductSnapshot;
import com.sellerscope.repository.ProductSnapshotRepository;
import com.sellerscope.repository.TrackedProductRepository;
import com.sellerscope.service.WbProductParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

class ProductUpdateSchedulerTest {

    @Mock
    private ProductSnapshotRepository productSnapshotRepository;
    @Mock
    private TrackedProductRepository trackedProductRepository;
    @Mock
    private WbProductParserService wbProductParserService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock rLock;

    @InjectMocks
    private ProductUpdateScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new ProductUpdateScheduler(
                productSnapshotRepository,
                trackedProductRepository,
                wbProductParserService,
                redissonClient
        );
    }

    @Test
    void updateTrackedProducts_shouldSaveChangedSnapshots() {
        String article = "123";
        ProductSnapshot changedSnapshot = ProductSnapshot.builder()
                .productId(article)
                .changed(true)
                .changedFields(Set.of("price"))
                .build();

        when(trackedProductRepository.findAllDistinctProductIds()).thenReturn(List.of(article));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock()).thenReturn(true);
        when(wbProductParserService.fetchSnapshotByArticle(article)).thenReturn(changedSnapshot);

        scheduler.updateTrackedProducts();

        verify(productSnapshotRepository, times(1)).save(changedSnapshot);
        verify(rLock, times(1)).unlock();
    }

    @Test
    void updateTrackedProducts_shouldNotSaveUnchangedSnapshots() {
        String article = "456";
        ProductSnapshot unchangedSnapshot = ProductSnapshot.builder()
                .productId(article)
                .changed(false)
                .build();

        when(trackedProductRepository.findAllDistinctProductIds()).thenReturn(List.of(article));
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock()).thenReturn(true);
        when(wbProductParserService.fetchSnapshotByArticle(article)).thenReturn(unchangedSnapshot);

        scheduler.updateTrackedProducts();

        verify(productSnapshotRepository, never()).save(any());
        verify(rLock, times(1)).unlock();
    }
}