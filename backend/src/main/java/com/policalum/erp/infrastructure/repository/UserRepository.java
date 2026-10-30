package com.policalum.erp.infrastructure.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ✅ Buscar por email
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.rol JOIN FETCH u.sede WHERE u.email = :email")
    Optional<User> findPerfilByEmail(@Param("email") String email);

    // ✅ Buscar por email SOLO si está activo
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.activo = true")
    Optional<User> findByEmailAndActivoTrue(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithActivoCheck(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.activo = true")
    boolean existsByEmailAndActivoTrue(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.activo = true AND u.rol.nombre = :rol")
    Optional<User> findByEmailAndRolAndActivoTrue(@Param("email") String email, @Param("rol") String rol);

    Optional<User> findFirstByActivoTrueAndRol_NombreOrderByEmailAsc(RolNombre nombre);
}