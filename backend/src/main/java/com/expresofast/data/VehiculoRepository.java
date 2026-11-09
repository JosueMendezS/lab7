package com.expresofast.data;

import com.expresofast.domain.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Integer> {

    boolean existsByPlaca(String placa);

    boolean existsByPlacaAndIdNot(String placa, Integer id);

    boolean existsByEmpresaId(Integer empresaId);
}
