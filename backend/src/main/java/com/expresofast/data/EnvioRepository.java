package com.expresofast.data;

import com.expresofast.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Integer> {

    @Query("SELECT e FROM Envio e " +
            "JOIN FETCH e.vehiculo v " +
            "JOIN FETCH v.empresa " +
            "JOIN FETCH e.conductor " +
            "ORDER BY e.id DESC")
    List<Envio> findAllOptimizado();

    @Query("SELECT e FROM Envio e " +
            "JOIN FETCH e.vehiculo " +
            "JOIN FETCH e.conductor " +
            "WHERE e.id = :id")
    Optional<Envio> findByIdOptimizado(@Param("id") Integer id);

    boolean existsByCodigoRastreo(String codigoRastreo);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Envio e SET e.estadoEnvio = :estado WHERE e.vehiculo.id = :vehiculoId")
    int actualizarEstadoPorVehiculo(@Param("vehiculoId") Integer vehiculoId,
            @Param("estado") String estado);
}