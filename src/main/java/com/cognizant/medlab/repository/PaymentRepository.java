package com.cognizant.medlab.repository;

import com.cognizant.medlab.domain.billing.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p WHERE p.invoice.id = :iid AND p.isDeleted = false")
    List<Payment> findByInvoiceId(@Param("iid") Long invoiceId);

    @Query("SELECT p FROM Payment p WHERE p.isDeleted = false")
    Page<Payment> findAllActive(Pageable pageable);
}
