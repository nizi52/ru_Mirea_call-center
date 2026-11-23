package ru.mirea.callcenter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.callcenter.domain.CallCenter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for study centers. */
public interface CallCenterRepository extends JpaRepository<CallCenter, UUID> {
    Optional<CallCenter> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<CallCenter> findAllByOrderByCodeAsc();
    List<CallCenter> findByActiveTrueOrderByCodeAsc();
}
