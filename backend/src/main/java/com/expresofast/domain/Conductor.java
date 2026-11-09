package com.expresofast.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Conductor")
public class Conductor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conductor_id")
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 50)
    private String apellidos;

    @Column(name = "licencia", nullable = false, unique = true, length = 20)
    private String licencia;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    // Lab 7: solo los conductores activos pueden asignarse a un vehiculo.
    @Column(name = "activo", nullable = false)
    private Boolean activo = Boolean.TRUE;

    @JsonIgnore
    @OneToMany(mappedBy = "conductor")
    private List<Envio> envios = new ArrayList<>();
}