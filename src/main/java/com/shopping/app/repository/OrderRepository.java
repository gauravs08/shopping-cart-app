package com.shopping.app.repository;

import com.shopping.app.entity.Order;
import com.shopping.app.entity.OrderStatus;
import com.shopping.app.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserAndStatus(User user, OrderStatus status, Pageable pageable);
}
