package com.example.employee_managment.repository;

import com.example.employee_managment.model.ExportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExportHistoryRepository extends JpaRepository<ExportHistory, Long> {
    
    Optional<ExportHistory> findByReferenceId(String referenceId);
    
    List<ExportHistory> findByStatus(ExportHistory.ExportStatus status);
    
    List<ExportHistory> findByUserId(String userId);
    
    List<ExportHistory> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT e FROM ExportHistory e WHERE e.status = 'COMPLETED' AND e.createdAt >= :since")
    List<ExportHistory> findCompletedExportsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(e) FROM ExportHistory e WHERE e.status = 'PROCESSING'")
    long countProcessingExports();
    
    @Query("SELECT e FROM ExportHistory e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<ExportHistory> findPendingExportsOrderByCreatedAt();
} 