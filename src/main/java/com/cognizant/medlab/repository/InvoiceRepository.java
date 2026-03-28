package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.billing.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumberAndIsDeletedFalse(String invoiceNumber);
    Optional<Invoice> findByAppointmentIdAndIsDeletedFalse(Long appointmentId);

    @Query("SELECT i FROM Invoice i WHERE i.isDeleted = false")
    Page<Invoice> findAllActive(Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.patient.id = :pid AND i.isDeleted = false")
    Page<Invoice> findByPatientId(@Param("pid") Long patientId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.status = :status AND i.isDeleted = false")
    Page<Invoice> findByStatus(@Param("status") Invoice.InvoiceStatus status, Pageable pageable);
}
