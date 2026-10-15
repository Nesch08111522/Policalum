package com.policalum.erp.infrastructure.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.policalum.erp.domain.model.Rol; 
@Repository
public interface  RolRepository  extends  JpaRepository<Rol,UUID>{

}
