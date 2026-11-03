package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.VapidClave;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VapidClaveRepository extends JpaRepository<VapidClave, Short> {
}
