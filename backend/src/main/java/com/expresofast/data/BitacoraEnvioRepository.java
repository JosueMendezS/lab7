package com.expresofast.data;

import com.expresofast.domain.BitacoraEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BitacoraEnvioRepository extends JpaRepository<BitacoraEnvio, Integer> {

    @Query("SELECT b FROM BitacoraEnvio b " +
            "JOIN FETCH b.usuario " +
            "WHERE b.envio.id = :envioId " +
            "ORDER BY b.fechaCambio DESC")
    List<BitacoraEnvio> findByEnvioId(@Param("envioId") Integer envioId);
}