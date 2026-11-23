package ru.mirea.callcenter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.callcenter.domain.Appeal;
import ru.mirea.callcenter.domain.AppealStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for appeals, smart specifications and compact analytics. */
public interface AppealRepository extends JpaRepository<Appeal, UUID>, JpaSpecificationExecutor<Appeal> {
    @Query("select count(a) from Appeal a where a.operatorId=:operatorId and a.id<>:excluded and a.archived=false and a.status in (ru.mirea.callcenter.domain.AppealStatus.NEW,ru.mirea.callcenter.domain.AppealStatus.IN_PROGRESS,ru.mirea.callcenter.domain.AppealStatus.ESCALATED)")
    long activeLoad(Integer operatorId, UUID excluded);
    boolean existsByExternalIdIgnoreCase(String externalId);
    boolean existsByExternalIdIgnoreCaseAndIdNot(String externalId, UUID id);
    Optional<Appeal> findByExternalIdIgnoreCase(String externalId);
    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndUrgentTrue();
    long countByArchivedFalseAndStatus(AppealStatus status);

    @Query("select coalesce(avg(s.impactScore), 0) from Appeal s where s.archived = false and s.impactScore is not null")
    BigDecimal impactScoreForActive();

    @Query("select coalesce(g.code, 'Без колл-центра'), count(s) from Appeal s left join s.center g " +
           "where s.archived = false group by g.code order by count(s) desc")
    List<Object[]> countActiveByCallCenter();

    @Query("select s.status, count(s) from Appeal s where s.archived = false group by s.status order by count(s) desc")
    List<Object[]> countActiveByStatus();

    @Query("select s.priorityLevel, count(s) from Appeal s where s.archived = false group by s.priorityLevel order by count(s) desc")
    List<Object[]> countActiveByPriority();
}
