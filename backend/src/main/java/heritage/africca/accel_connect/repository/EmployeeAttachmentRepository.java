package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.outils.EmpAttachmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeAttachmentRepository extends JpaRepository<EmployeeAttachment, EmpAttachmentId> {

    List<EmployeeAttachment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<EmployeeAttachment> findByUserIdAndAttachId(Long userId, Integer attachId);

    @Query("SELECT MAX(a.attachId) FROM EmployeeAttachment a WHERE a.userId = :userId")
    Optional<Integer> findMaxAttachIdByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndAttachId(Long userId, Integer attachId);

    List<EmployeeAttachment> findByScreen(String screen);

    Optional<EmployeeAttachment> findByScreenAndAttachId(String screen, Integer attachId);
}
