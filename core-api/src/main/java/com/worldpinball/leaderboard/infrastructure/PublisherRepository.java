package com.worldpinball.leaderboard.infrastructure;

import com.worldpinball.leaderboard.domain.entities.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, UUID> {}
