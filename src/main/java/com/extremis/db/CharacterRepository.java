package com.extremis.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterRepository extends JpaRepository<CharacterEntity, String> {
    List<CharacterEntity> findByRecruteTrue();

    List<CharacterEntity> findByRecruteFalse();
}
