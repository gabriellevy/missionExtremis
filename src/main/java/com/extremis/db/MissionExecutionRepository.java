package com.extremis.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MissionExecutionRepository extends JpaRepository<MissionExecutionEntity, Long> {
    Optional<MissionExecutionEntity> findFirstByStatusOrderByIdDesc(MissionExecutionEntity.Status status);

    List<MissionExecutionEntity> findAllByStatusOrderByIdAsc(MissionExecutionEntity.Status status);
}
