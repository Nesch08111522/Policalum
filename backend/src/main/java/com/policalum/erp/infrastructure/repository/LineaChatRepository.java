package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.domain.model.LineaChat;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineaChatRepository extends JpaRepository<LineaChat, UUID> {
    List<LineaChat> findByConversacionIdOrderByCreatedAtAsc(UUID conversacionId);
    void deleteByConversacionId(UUID conversacionId);
}
