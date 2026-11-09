-- ExpresoFast - Laboratorio 7
-- Extension del esquema necesaria para las reglas de negocio certificadas por
-- la suite de pruebas:
--   * Conductor.activo      -> solo conductores activos pueden asignarse a un vehiculo.
--   * Vehiculo.conductor_id -> conductor asignado actualmente al vehiculo (opcional).

ALTER TABLE Conductor
    ADD activo BIT NOT NULL CONSTRAINT DF_Conductor_Activo DEFAULT 1;
GO

ALTER TABLE Vehiculo
    ADD conductor_id INT NULL
        CONSTRAINT FK_Vehiculo_Conductor REFERENCES Conductor(conductor_id);
GO
