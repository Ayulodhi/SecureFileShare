package com.securefileshare.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing an established friendship in the FRIENDSHIPS table.
 * WHAT: Stores canonical ordered pairs (user_id_1 < user_id_2) to prevent duplicate rows.
 * WHY: Enables fast friendship checks between users.
 */
@Entity
@Table(name = "friendships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id_1", "user_id_2"}, name = "uk_friendship_canonical")
})
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_1", nullable = false)
    private User userId1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_2", nullable = false)
    private User userId2;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Friendship() {}

    public Friendship(User userA, User userB) {
        if (compareUuids(userA.getId(), userB.getId()) < 0) {
            this.userId1 = userA;
            this.userId2 = userB;
        } else {
            this.userId1 = userB;
            this.userId2 = userA;
        }
        this.createdAt = Instant.now();
    }

    private static int compareUuids(UUID u1, UUID u2) {
        long m1 = u1.getMostSignificantBits();
        long m2 = u2.getMostSignificantBits();
        int cmp = Long.compareUnsigned(m1, m2);
        if (cmp != 0) {
            return cmp;
        }
        long l1 = u1.getLeastSignificantBits();
        long l2 = u2.getLeastSignificantBits();
        return Long.compareUnsigned(l1, l2);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public User getUserId1() { return userId1; }
    public User getUserId2() { return userId2; }
    public Instant getCreatedAt() { return createdAt; }
}
