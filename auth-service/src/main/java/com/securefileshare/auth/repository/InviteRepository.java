package com.securefileshare.auth.repository;

import com.securefileshare.auth.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InviteRepository extends JpaRepository<Invite, UUID> {
    Optional<Invite> findByInviteCode(String inviteCode);
}
