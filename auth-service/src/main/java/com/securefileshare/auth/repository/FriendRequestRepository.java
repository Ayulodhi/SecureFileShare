package com.securefileshare.auth.repository;

import com.securefileshare.auth.entity.FriendRequest;
import com.securefileshare.auth.entity.RequestStatus;
import com.securefileshare.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {
    Optional<FriendRequest> findByRequesterAndAddressee(User requester, User addressee);
    List<FriendRequest> findByAddresseeAndStatus(User addressee, RequestStatus status);
    List<FriendRequest> findByRequesterAndStatus(User requester, RequestStatus status);
    long countByRequesterAndStatus(User requester, RequestStatus status);
}
