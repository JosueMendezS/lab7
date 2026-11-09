const API_ENVIOS = `${API_BASE_URL}/api/envios`;
const API_VEHICULOS = `${API_BASE_URL}/api/vehiculos`;

let enviosCache = [];
let filtroActivo = 'TODOS';
let bitacoraCache = [];

const gridEnvios = document.getElementById('gridEnvios');
const boardSubtitulo = document.getElementById('boardSubtitulo');
const formEnvio = document.getElementById('formEnvio');
const formFeedback = document.getElementById('formFeedback');

const PILL_CLASE = {
    ENTREGADO: 'pill-status--entregado',
    EN_TRANSITO: 'pill-status--en-transito',
    PENDIENTE: 'pill-status--pendiente',
    CANCELADO: 'pill-status--cancelado',
};

const PILL_TEXTO = {
    ENTREGADO: 'Entregado',
    EN_TRANSITO: 'En tránsito',
    PENDIENTE: 'Pendiente',
    CANCELADO: 'Cancelado',
};

// ---------------------------------------------------------------------------
// Sesión y render condicional por rol
// ---------------------------------------------------------------------------
function inicializarSesion() {
    document.getElementById('nombreUsuarioActual').textContent = obtenerUsername();
    const roles = obtenerRoles().map((r) => r.replace('ROLE_', ''));
    document.getElementById('rolUsuarioActual').textContent = roles.join(', ') || 'Sin rol';

    // ROLE_CONDUCTOR: oculta el formulario de creación de envíos y la pestaña de flota
    if (esConductor() && !esAdmin() && !esOperador()) {
        document.getElementById('panelIntake').hidden = true;
    }

    document.getElementById('tabFlota').hidden = !esAdmin();

    document.getElementById('btnLogout').addEventListener('click', cerrarSesion);
}

document.querySelectorAll('.vista-tab').forEach((boton) => {
    boton.addEventListener('click', () => {
        if (boton.hidden) return;
        document.querySelectorAll('.vista-tab').forEach((b) => b.classList.remove('is-active'));
        boton.classList.add('is-active');

        const vista = boton.dataset.vista;
        document.getElementById('vistaEnvios').hidden = vista !== 'envios';
        document.getElementById('vistaFlota').hidden = vista !== 'flota';

        if (vista === 'flota') cargarFlota();
    });
});

// ---------------------------------------------------------------------------
// Envíos
// ---------------------------------------------------------------------------
async function cargarEnvios() {
    boardSubtitulo.textContent = 'Cargando manifiesto de envíos…';
    try {
        const respuesta = await fetchWithAuth(`${API_ENVIOS}/optimizados`);
        enviosCache = await respuesta.json();
        actualizarResumen();
        renderizarGrid();
    } catch (error) {
        boardSubtitulo.textContent = `No se pudo cargar el manifiesto: ${error.message}`;
        console.error('Error al cargar envios:', error);
    }
}

function renderizarGrid() {
    const enviosFiltrados =
        filtroActivo === 'TODOS'
            ? enviosCache
            : enviosCache.filter((e) => e.estadoEnvio === filtroActivo);

    boardSubtitulo.textContent = `${enviosFiltrados.length} envío(s) en el filtro actual`;

    gridEnvios.innerHTML = '';

    if (enviosFiltrados.length === 0) {
        gridEnvios.innerHTML = '<p class="board__empty">No hay envíos que coincidan con este filtro.</p>';
        return;
    }

    enviosFiltrados.forEach((envio) => {
        gridEnvios.appendChild(crearTarjetaEnvio(envio));
    });
}

function crearTarjetaEnvio(envio) {
    const articulo = document.createElement('article');
    articulo.className = 'waybill';

    const pillClase = PILL_CLASE[envio.estadoEnvio] || 'pill-status--pendiente';
    const pillTexto = PILL_TEXTO[envio.estadoEnvio] || envio.estadoEnvio;

    articulo.innerHTML = `
    <div class="waybill__head">
      <span class="waybill__codigo">${envio.codigoRastreo}</span>
      <span class="pill-status ${pillClase}">${pillTexto}</span>
    </div>
    <p class="waybill__destino">${envio.direccionDestino}</p>
    <div class="waybill__datos">
      <span>${Number(envio.pesoKg).toFixed(2)} kg</span>
      <span>₡${Number(envio.costo).toFixed(2)}</span>
    </div>
    <p class="waybill__asignacion">veh: ${envio.placaVehiculo || '—'} · conductor: ${envio.nombreConductor || '—'}</p>
    <div class="waybill__acciones" data-envio-id="${envio.id}"></div>
  `;

    const acciones = articulo.querySelector('.waybill__acciones');
    acciones.appendChild(crearBotonesAccion(envio));

    return articulo;
}

function crearBotonesAccion(envio) {
    const contenedor = document.createElement('div');
    contenedor.className = 'waybill__acciones-inner';
    contenedor.style.display = 'flex';
    contenedor.style.gap = '0.5rem';
    contenedor.style.flexWrap = 'wrap';

    // Matriz RBAC: PATCH /api/envios/{id}/estado -> ADMIN, CONDUCTOR
    const puedeCambiarEstado = esAdmin() || esConductor();

    if (puedeCambiarEstado && envio.estadoEnvio === 'PENDIENTE') {
        contenedor.appendChild(
            crearBoton('Marcar en tránsito', () => cambiarEstado(envio.id, 'EN_TRANSITO'))
        );
    }

    if (puedeCambiarEstado && envio.estadoEnvio === 'EN_TRANSITO') {
        contenedor.appendChild(
            crearBoton('Marcar entregado', () => cambiarEstado(envio.id, 'ENTREGADO'))
        );
    }

    // Matriz RBAC: GET /api/envios/{id}/bitacora -> ADMIN, OPERADOR
    if (esAdmin() || esOperador()) {
        contenedor.appendChild(crearBoton('Ver bitácora', () => abrirModalBitacora(envio)));
    }

    return contenedor;
}

function crearBoton(texto, onClick) {
    const boton = document.createElement('button');
    boton.type = 'button';
    boton.className = 'btn-accion';
    boton.textContent = texto;
    boton.addEventListener('click', onClick);
    return boton;
}

function actualizarResumen() {
    const conteo = { PENDIENTE: 0, EN_TRANSITO: 0, ENTREGADO: 0 };
    enviosCache.forEach((e) => {
        if (conteo[e.estadoEnvio] !== undefined) conteo[e.estadoEnvio]++;
    });
    document.getElementById('countPendiente').textContent = conteo.PENDIENTE;
    document.getElementById('countTransito').textContent = conteo.EN_TRANSITO;
    document.getElementById('countEntregado').textContent = conteo.ENTREGADO;
}

document.querySelectorAll('.filtro-tab').forEach((boton) => {
    boton.addEventListener('click', () => {
        document.querySelectorAll('.filtro-tab').forEach((b) => b.classList.remove('is-active'));
        boton.classList.add('is-active');
        filtroActivo = boton.dataset.filtro;
        renderizarGrid();
    });
});

formEnvio.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    formFeedback.textContent = '';
    formFeedback.className = 'form-feedback';

    const payload = {
        codigoRastreo: document.getElementById('codigoRastreo').value.trim(),
        direccionDestino: document.getElementById('direccionDestino').value.trim(),
        pesoKg: parseFloat(document.getElementById('pesoKg').value),
        costo: parseFloat(document.getElementById('costo').value),
        vehiculoId: parseInt(document.getElementById('vehiculoId').value, 10),
        conductorId: parseInt(document.getElementById('conductorId').value, 10),
    };

    try {
        await fetchWithAuth(API_ENVIOS, {
            method: 'POST',
            body: JSON.stringify(payload),
        });

        formFeedback.textContent = 'Envío registrado correctamente.';
        formFeedback.classList.add('ok');
        formEnvio.reset();
        await cargarEnvios();
    } catch (error) {
        formFeedback.textContent = `No se pudo registrar el envío: ${error.message}`;
        formFeedback.classList.add('error');
        console.error('Error al registrar envio:', error);
    }
});

async function cambiarEstado(envioId, nuevoEstado) {
    try {
        await fetchWithAuth(`${API_ENVIOS}/${envioId}/estado`, {
            method: 'PATCH',
            body: JSON.stringify({ nuevoEstado, observaciones: null }),
        });
        await cargarEnvios();
    } catch (error) {
        alert(`No se pudo actualizar el estado del envío: ${error.message}`);
        console.error('Error al actualizar estado:', error);
    }
}

// ---------------------------------------------------------------------------
// Modal de bitácora de auditoría
// ---------------------------------------------------------------------------
const modalBitacora = document.getElementById('modalBitacora');
const modalBitacoraBody = document.getElementById('modalBitacoraBody');
const modalCodigoEnvio = document.getElementById('modalCodigoEnvio');
const filtroFechaInicio = document.getElementById('filtroFechaInicio');
const filtroFechaFin = document.getElementById('filtroFechaFin');

async function abrirModalBitacora(envio) {
    modalCodigoEnvio.textContent = envio.codigoRastreo;
    modalBitacoraBody.innerHTML = '<p class="board__empty">Cargando historial…</p>';
    filtroFechaInicio.value = '';
    filtroFechaFin.value = '';
    modalBitacora.hidden = false;

    try {
        const respuesta = await fetchWithAuth(`${API_ENVIOS}/${envio.id}/bitacora`);
        bitacoraCache = await respuesta.json();
        renderizarBitacora();
    } catch (error) {
        modalBitacoraBody.innerHTML = `<p class="board__empty">No se pudo cargar la bitácora: ${error.message}</p>`;
        console.error('Error al cargar bitacora:', error);
    }
}

function renderizarBitacora() {
    let entradas = bitacoraCache;

    if (filtroFechaInicio.value) {
        const desde = new Date(filtroFechaInicio.value);
        entradas = entradas.filter((b) => new Date(b.fechaCambio) >= desde);
    }
    if (filtroFechaFin.value) {
        const hasta = new Date(filtroFechaFin.value);
        hasta.setHours(23, 59, 59, 999);
        entradas = entradas.filter((b) => new Date(b.fechaCambio) <= hasta);
    }

    if (entradas.length === 0) {
        modalBitacoraBody.innerHTML = '<p class="board__empty">No hay entradas de bitácora en este rango.</p>';
        return;
    }

    modalBitacoraBody.innerHTML = entradas
        .map(
            (b) => `
    <div class="bitacora-item">
      <p><strong>${PILL_TEXTO[b.estadoAnterior] || b.estadoAnterior}</strong> → <strong>${PILL_TEXTO[b.estadoNuevo] || b.estadoNuevo}</strong></p>
      <p>${new Date(b.fechaCambio).toLocaleString('es-CR')} · ${b.usuario || '—'}</p>
      ${b.observaciones ? `<p>${b.observaciones}</p>` : ''}
    </div>
  `
        )
        .join('');
}

filtroFechaInicio.addEventListener('change', renderizarBitacora);
filtroFechaFin.addEventListener('change', renderizarBitacora);

document.getElementById('btnLimpiarFiltroFechas').addEventListener('click', () => {
    filtroFechaInicio.value = '';
    filtroFechaFin.value = '';
    renderizarBitacora();
});

document.getElementById('btnCerrarModal').addEventListener('click', () => {
    modalBitacora.hidden = true;
});

modalBitacora.addEventListener('click', (evento) => {
    if (evento.target === modalBitacora) modalBitacora.hidden = true;
});

// ---------------------------------------------------------------------------
// Flota (solo ROLE_ADMIN según la matriz RBAC)
// ---------------------------------------------------------------------------
const formVehiculo = document.getElementById('formVehiculo');
const vehiculoFeedback = document.getElementById('vehiculoFeedback');
const tablaFlotaBody = document.getElementById('tablaFlotaBody');

async function cargarFlota() {
    tablaFlotaBody.innerHTML = '<tr><td colspan="6">Cargando…</td></tr>';
    try {
        const respuesta = await fetchWithAuth(API_VEHICULOS);
        const vehiculos = await respuesta.json();
        renderizarFlota(vehiculos);
    } catch (error) {
        tablaFlotaBody.innerHTML = `<tr><td colspan="6">Error al cargar flota: ${error.message}</td></tr>`;
        console.error('Error al cargar flota:', error);
    }
}

function renderizarFlota(vehiculos) {
    if (vehiculos.length === 0) {
        tablaFlotaBody.innerHTML = '<tr><td colspan="6">No hay vehículos registrados.</td></tr>';
        return;
    }

    tablaFlotaBody.innerHTML = vehiculos
        .map(
            (v) => `
    <tr>
      <td>${v.id}</td>
      <td>${v.placa}</td>
      <td>${Number(v.capacidadKg).toFixed(2)}</td>
      <td>${v.estado}</td>
      <td>${v.nombreEmpresa || v.empresaId}</td>
      <td><button type="button" class="btn-accion" data-eliminar="${v.id}">Eliminar</button></td>
    </tr>
  `
        )
        .join('');

    tablaFlotaBody.querySelectorAll('[data-eliminar]').forEach((boton) => {
        boton.addEventListener('click', () => eliminarVehiculo(boton.dataset.eliminar));
    });
}

formVehiculo.addEventListener('submit', async (evento) => {
    evento.preventDefault();
    vehiculoFeedback.textContent = '';
    vehiculoFeedback.className = 'form-feedback';

    const payload = {
        placa: document.getElementById('vehPlaca').value.trim(),
        capacidadKg: parseFloat(document.getElementById('vehCapacidad').value),
        estado: document.getElementById('vehEstado').value,
        empresaId: parseInt(document.getElementById('vehEmpresaId').value, 10),
    };

    try {
        await fetchWithAuth(API_VEHICULOS, {
            method: 'POST',
            body: JSON.stringify(payload),
        });

        vehiculoFeedback.textContent = 'Vehículo agregado correctamente.';
        vehiculoFeedback.classList.add('ok');
        formVehiculo.reset();
        await cargarFlota();
    } catch (error) {
        vehiculoFeedback.textContent = `No se pudo agregar el vehículo: ${error.message}`;
        vehiculoFeedback.classList.add('error');
        console.error('Error al agregar vehiculo:', error);
    }
});

async function eliminarVehiculo(id) {
    if (!confirm('¿Eliminar este vehículo?')) return;
    try {
        await fetchWithAuth(`${API_VEHICULOS}/${id}`, { method: 'DELETE' });
        await cargarFlota();
    } catch (error) {
        alert(`No se pudo eliminar el vehículo: ${error.message}`);
        console.error('Error al eliminar vehiculo:', error);
    }
}

// ---------------------------------------------------------------------------
// Arranque
// ---------------------------------------------------------------------------
inicializarSesion();
cargarEnvios();
