package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface SedeRepository extends JpaRepository< Sede, UUID> {

    Page<Sede> findByActiva(Boolean activa, Pageable pageable);
}
