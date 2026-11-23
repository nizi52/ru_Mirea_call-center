package ru.mirea.callcenter.repository;
import ru.mirea.callcenter.domain.CallOperator;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface CallOperatorRepository extends JpaRepository<CallOperator,Integer>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<CallOperator> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from CallOperator o where o.id=:id")
 Optional<CallOperator> lockById(Integer id);
}
