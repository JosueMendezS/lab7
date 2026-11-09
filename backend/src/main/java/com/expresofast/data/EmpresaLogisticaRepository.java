package com.expresofast.data;

import com.expresofast.domain.EmpresaLogistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpresaLogisticaRepository extends JpaRepository<EmpresaLogistica, Integer> {

    boolean existsByNombre(String nombre);

    boolean existsByCedulaJuridica(String cedulaJuridica);

    boolean existsByNombreAndIdNot(String nombre, Integer id);

    boolean existsByCedulaJuridicaAndIdNot(String cedulaJuridica, Integer id);
}
