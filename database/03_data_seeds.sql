-- Contraseña en texto plano para los 3: 'Password123!'
INSERT INTO Usuario (username, password_hash, nombre_completo, email, activo) VALUES
('admin',      '$2b$10$kW7e.XejpIU5Ic5JfgPnKO0bkFl8OPa8GwaGWwtkwOo8RQZNc6UlK', 'Carlos Alvarado',  'admin@expresofast.cr',      1),
('operador1',  '$2b$10$OqmKHdLqMD1nxD8cZUN32.NiI/EQSseh4STzZFCK..Y0c2PpOQoYe', 'Maria Rodriguez',  'operador1@expresofast.cr',  1),
('conductor1', '$2b$10$htOFEoFux7KDCA19u1i58u0gq/OZ95VWXHCX1VXFaj.1IXwodYpY.', 'Jose Fernandez',   'conductor1@expresofast.cr', 1);

INSERT INTO Rol (nombre_rol) VALUES
('ROLE_ADMIN'), ('ROLE_OPERADOR'), ('ROLE_CONDUCTOR');

-- usuario_id 1=admin, 2=operador1, 3=conductor1 | rol_id 1=ADMIN, 2=OPERADOR, 3=CONDUCTOR
INSERT INTO UsuarioRol (usuario_id, rol_id) VALUES
(1, 1),
(2, 2),
(3, 3);