package com.shopping.app.repository;

import com.shopping.app.entity.Cart;
import com.shopping.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(UUID userId);
}
