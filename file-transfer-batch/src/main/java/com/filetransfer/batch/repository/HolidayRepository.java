package com.filetransfer.batch.repository;

import com.filetransfer.batch.entity.Holiday;
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
    
    Holiday findByTenantIdAndHolidayDate(String tenantId, LocalDate holidayDate);
    
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Holiday h WHERE h.tenantId = :tenantId AND h.holidayDate = :date")
    boolean isHoliday(@Param("tenantId") String tenantId, @Param("date") LocalDate date);
    
    void deleteByTenantIdAndHolidayDate(String tenantId, LocalDate holidayDate);
    
    @Query("SELECT h FROM Holiday h WHERE h.tenantId = :tenantId AND (h.holidayName LIKE %:searchTerm% OR h.description LIKE %:searchTerm%)")
    List<Holiday> searchHolidays(@Param("tenantId") String tenantId, @Param("searchTerm") String searchTerm);
}