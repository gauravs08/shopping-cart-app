package com.shopping.app.repository;

import com.shopping.app.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.order.user.id = :userId " +
            "AND oi.product.id = :productId AND oi.order.status = 'DELIVERED'")
    boolean existsByUserIdAndProductIdAndOrderDelivered(
            @Param("userId") UUID userId, @Param("productId") UUID productId);
}
