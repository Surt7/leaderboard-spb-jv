package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByDisplayName(String displayName);
    List<UserProfile> findByProfilePublicTrue();
}
