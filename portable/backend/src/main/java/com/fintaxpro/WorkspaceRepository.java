package com.fintaxpro;
import java.util.*;import jakarta.persistence.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface WorkspaceRepository extends JpaRepository<WorkspaceRecord,String>{@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select w from WorkspaceRecord w where w.userId=:uid") Optional<WorkspaceRecord> locked(@Param("uid")String uid);}
