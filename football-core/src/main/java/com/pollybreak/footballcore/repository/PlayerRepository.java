package com.pollybreak.footballcore.repository;

import com.pollybreak.footballcore.domain.entity.Player;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    List<Player> findAll();

    @EntityGraph(attributePaths = "user")
    List<Player> findAllByActiveTrueOrderByFirstNameAscLastNameAsc();

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<Player> findById(Long id);

    Optional<Player> findByUserId(Long userId);
}
