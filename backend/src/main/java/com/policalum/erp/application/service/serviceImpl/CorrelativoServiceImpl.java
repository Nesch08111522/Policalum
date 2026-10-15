package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.domain.model.Correlativo;
import com.policalum.erp.infrastructure.repository.CorrelativoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorrelativoServiceImpl implements CorrelativoService {

    private final CorrelativoRepository correlativoRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long siguiente(String clave) {
        correlativoRepository.crearSiNoExiste(clave);
        Correlativo c = correlativoRepository.findParaActualizar(clave)
                .orElseThrow(() -> new IllegalStateException("No se pudo reservar el correlativo " + clave));
        long siguiente = (c.getUltimo() == null ? 0 : c.getUltimo()) + 1;
        c.setUltimo(siguiente);
        correlativoRepository.save(c);
        return siguiente;
    }
}
