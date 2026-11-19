package com.securefileshare.auth.repository;

import com.securefileshare.auth.entity.Friendship;
import com.securefileshare.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("SELECT f FROM Friendship f WHERE (f.userId1 = :u1 AND f.userId2 = :u2) OR (f.userId1 = :u2 AND f.userId2 = :u1)")
    Optional<Friendship> findCanonical(@Param("u1") User u1, @Param("u2") User u2);

    @Query("SELECT f FROM Friendship f WHERE f.userId1 = :user OR f.userId2 = :user")
    List<Friendship> findAllForUser(@Param("user") User user);
}
