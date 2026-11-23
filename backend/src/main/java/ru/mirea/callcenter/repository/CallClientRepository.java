package ru.mirea.callcenter.repository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import ru.mirea.callcenter.domain.CallClient;
import java.util.Optional;
public interface CallClientRepository extends JpaRepository<CallClient,Integer>, JpaSpecificationExecutor<CallClient> {
 @Query(value="select * from clients where normalize_client_phone(phone)=normalize_client_phone(:phone) order by id limit 1",nativeQuery=true)
 Optional<CallClient> findByPhone(@Param("phone") String phone);
 @Query(value="select * from clients where normalize_client_phone(phone)=normalize_client_phone(:phone) and id<>:id order by id limit 1",nativeQuery=true)
 Optional<CallClient> findOtherByNormalizedPhone(@Param("phone") String phone,@Param("id") Integer id);
}
