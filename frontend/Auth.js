
const API_BASE_URL = 'http://localhost:8080';

const CLAVE_TOKEN = 'jwt_token';
const CLAVE_USERNAME = 'jwt_username';
const CLAVE_ROLES = 'jwt_roles';

function obtenerToken() {
    return localStorage.getItem(CLAVE_TOKEN);
}

function obtenerUsername() {
    return localStorage.getItem(CLAVE_USERNAME) || '';
}

function obtenerRoles() {
    try {
        return JSON.parse(localStorage.getItem(CLAVE_ROLES)) || [];
    } catch (error) {
        return [];
    }
}

function tieneRol(rol) {
    return obtenerRoles().includes(rol);
}

function esAdmin() {
    return tieneRol('ROLE_ADMIN');
}

function esOperador() {
    return tieneRol('ROLE_OPERADOR');
}

function esConductor() {
    return tieneRol('ROLE_CONDUCTOR');
}


function guardarSesion(authResponseDTO) {
    localStorage.setItem(CLAVE_TOKEN, authResponseDTO.token);
    localStorage.setItem(CLAVE_USERNAME, authResponseDTO.username);
    localStorage.setItem(CLAVE_ROLES, JSON.stringify(authResponseDTO.roles || []));
}

function limpiarSesion() {
    localStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem(CLAVE_USERNAME);
    localStorage.removeItem(CLAVE_ROLES);
}

function cerrarSesion() {
    limpiarSesion();
    window.location.href = 'login.html';
}

function exigirSesion() {
    if (!obtenerToken()) {
        window.location.href = 'login.html';
    }
}


async function fetchWithAuth(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${obtenerToken()}`,
        ...(options.headers || {}),
    };

    const respuesta = await fetch(url, { ...options, headers });

    if (respuesta.status === 401 || respuesta.status === 403) {
        limpiarSesion();
        window.location.href = 'login.html';
        throw new Error('Sesión expirada o sin permisos. Redirigiendo al login…');
    }

    if (!respuesta.ok) {
        const cuerpo = await respuesta.json().catch(() => null);
        let mensaje = (cuerpo && (cuerpo.detail || cuerpo.message)) || `El servidor respondió con estado ${respuesta.status}`;

        if (cuerpo && Array.isArray(cuerpo.errores) && cuerpo.errores.length > 0) {
            const detalle = cuerpo.errores.map((e) => `${e.campo}: ${e.mensaje}`).join(' · ');
            mensaje = `${mensaje} (${detalle})`;
        }

        throw new Error(mensaje);
    }

    return respuesta;
}