package com.filetransfer.web.repository;

import com.filetransfer.web.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    
    List<Holiday> findByTenantId(String tenantId);
    
    List<Holiday> findByTenantIdAndHolidayDateBetween(String tenantId, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT h FROM Holiday h WHERE h.tenantId = :tenantId AND h.holidayDate = :date")
    Holiday findByTenantIdAndDate(@Param("tenantId") String tenantId, @Param("date") LocalDate date);
    
    @Query("SELECT COUNT(h) > 0 FROM Holiday h WHERE h.tenantId = :tenantId AND h.holidayDate = :date")
    boolean isHoliday(@Param("tenantId") String tenantId, @Param("date") LocalDate date);
    
    @Query("SELECT h FROM Holiday h WHERE h.tenantId = :tenantId AND h.holidayName LIKE %:searchTerm%")
    List<Holiday> searchHolidays(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
    
    void deleteByTenantIdAndHolidayDate(String tenantId, LocalDate holidayDate);
}