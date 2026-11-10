// Consumidor Web - Gestion de Prestamos y Devoluciones

// Rutas de comunicacion con los servicios
const API_DEVOLUCIONES = "http://localhost:9010/api/devoluciones";
const API_SOLICITUDES = "http://localhost:8082/api/solicitudes";

// Consultas de inventario y usuarios a traves del servicio 4 para evitar bloqueos de CORS
const API_INVENTARIO = "http://localhost:9010/api/devoluciones/inventario";
const API_USUARIOS = "http://localhost:9010/api/devoluciones/usuarios";

// Helper para obtener elementos del DOM
function $(id) {
    return document.getElementById(id);
}

// Generador simple de identificadores UUID
function generarUuid() {
    if (window.crypto && crypto.randomUUID) {
        return crypto.randomUUID();
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Muestra mensajes de estado en la interfaz
function mostrarMensaje(idElemento, texto, tipo = "exito") {
    const el = $(idElemento);
    if (!el) return;
    el.style.display = "block";
    el.className = "mensaje-box " + tipo;
    el.textContent = texto;
}

// Oculta un mensaje de estado
function ocultarMensaje(idElemento) {
    const el = $(idElemento);
    if (el) el.style.display = "none";
}

// Peticion HTTP con manejo de JSON y respuestas de error
async function pedirJson(url, opciones = {}) {
    const respuesta = await fetch(url, {
        ...opciones,
        headers: {
            "Accept": "application/json",
            ...(opciones.headers || {})
        }
    });

    const texto = await respuesta.text();
    let datos = null;
    if (texto) {
        try {
            datos = JSON.parse(texto);
        } catch {
            datos = { mensaje: texto };
        }
    }

    if (!respuesta.ok) {
        let mensaje = "Error en la comunicacion (" + respuesta.status + ")";
        if (datos) {
            mensaje = datos.mensaje || datos.message || datos.error || mensaje;
        }
        const error = new Error(mensaje);
        error.status = respuesta.status;
        throw error;
    }

    return datos;
}

// 1. Inventario Deportivo
async function consultarArticulo() {
    const id = $("articuloId").value.trim();
    const detalle = $("detalleArticulo");
    if (!id) return;

    try {
        ocultarMensaje("mensajeArticulo");
        const articulo = await pedirJson(`${API_INVENTARIO}/${id}`);

        $("artCodigo").textContent = articulo.codigo || "-";
        $("artNombre").textContent = articulo.nombre || "-";
        $("artDeporte").textContent = articulo.deporte || "-";
        $("artBuenEstado").textContent = articulo.cantidadBuenEstado ?? 0;
        $("artMantenimiento").textContent = articulo.cantidadMantenimiento ?? 0;
        $("artDanadas").textContent = articulo.cantidadDanada ?? 0;

        detalle.style.display = "grid";

        // Sugerir datos en los formularios inferiores
        if ($("solIdArticulo")) $("solIdArticulo").value = articulo.id || id;
        if ($("devCodigoArticulo")) $("devCodigoArticulo").value = articulo.codigo || "";

    } catch (error) {
        if (detalle) detalle.style.display = "none";
        mostrarMensaje("mensajeArticulo", "No se encontro el articulo: " + error.message, "error");
    }
}

async function cargarCatalogoCompleto() {
    const contenedor = $("contenedorTablaArticulos");
    const cuerpo = $("cuerpoTablaArticulos");
    if (!contenedor || !cuerpo) return;

    if (contenedor.style.display === "block") {
        contenedor.style.display = "none";
        return;
    }

    try {
        ocultarMensaje("mensajeArticulo");
        const lista = await pedirJson(API_INVENTARIO);
        cuerpo.innerHTML = "";

        if (!lista || lista.length === 0) {
            cuerpo.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--muted);">No hay articulos registrados.</td></tr>`;
            contenedor.style.display = "block";
            return;
        }

        lista.forEach(item => {
            const fila = document.createElement("tr");
            fila.innerHTML = `
                <td>${item.id}</td>
                <td><strong>${item.codigo}</strong></td>
                <td>${item.nombre}</td>
                <td>${item.deporte}</td>
                <td style="color: var(--success); font-weight: bold;">${item.cantidadBuenEstado}</td>
                <td style="color: var(--warning);">${item.cantidadMantenimiento}</td>
                <td style="color: var(--danger);">${item.cantidadDanada}</td>
                <td>
                    <button type="button" class="btn btn-outline btn-sm" onclick="seleccionarArticulo(${item.id}, '${item.codigo}')">Elegir</button>
                </td>
            `;
            cuerpo.appendChild(fila);
        });

        contenedor.style.display = "block";
    } catch (error) {
        mostrarMensaje("mensajeArticulo", "Error al cargar catalogo: " + error.message, "error");
    }
}

function seleccionarArticulo(id, codigo) {
    $("articuloId").value = id;
    consultarArticulo();
    if ($("solIdArticulo")) $("solIdArticulo").value = id;
    if ($("devCodigoArticulo")) $("devCodigoArticulo").value = codigo;
}

// 2. Consulta de Usuario
async function consultarUsuario() {
    const id = $("usuarioId").value.trim();
    const detalle = $("detalleUsuario");
    if (!id) return;

    try {
        ocultarMensaje("mensajeUsuario");
        const usuario = await pedirJson(`${API_USUARIOS}/${id}`);

        $("usrMatricula").textContent = usuario.matricula || "-";
        $("usrNombre").textContent = usuario.nombre || "-";
        $("usrCorreo").textContent = usuario.correo || "-";
        $("usrTipo").textContent = usuario.tipo || "-";
        $("usrEstado").textContent = usuario.estado || "-";
        $("usrLimite").textContent = usuario.limitePrestamos ?? "-";

        detalle.style.display = "grid";

        if ($("solIdUsuario")) $("solIdUsuario").value = usuario.id || id;
        if ($("devIdUsuario")) $("devIdUsuario").value = usuario.id || id;

    } catch (error) {
        if (detalle) detalle.style.display = "none";
        mostrarMensaje("mensajeUsuario", "No se encontro el usuario: " + error.message, "error");
    }
}

// 3. Solicitud de Prestamo
async function registrarSolicitud() {
    const idArticulo = parseInt($("solIdArticulo").value, 10);
    const idUsuario = parseInt($("solIdUsuario").value, 10);
    const cantidad = parseInt($("solCantidad").value, 10);
    const fechaUso = $("solFechaUso").value;
    const btn = $("btnSolicitar");

    const payload = {
        idOperacion: generarUuid(),
        idArticulo: idArticulo,
        idUsuario: idUsuario,
        cantidad: cantidad,
        fechaUso: fechaUso
    };

    try {
        btn.disabled = true;
        mostrarMensaje("mensajeSolicitud", "Registrando prestamo...", "info");

        const respuesta = await pedirJson(API_SOLICITUDES, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const idSolicitud = respuesta.id || respuesta.idOperacion || payload.idOperacion;
        mostrarMensaje("mensajeSolicitud", `Prestamo registrado con exito. Solicitud #${idSolicitud}`, "exito");

        // Rellenar datos en la seccion de devolucion
        if ($("devIdSolicitud")) $("devIdSolicitud").value = idSolicitud;
        if ($("devIdUsuario")) $("devIdUsuario").value = idUsuario;

        const cod = $("artCodigo")?.textContent;
        if (cod && cod !== "-" && $("devCodigoArticulo")) {
            $("devCodigoArticulo").value = cod;
        }

        // Actualizar datos del articulo para ver el nuevo stock
        consultarArticulo();

    } catch (error) {
        mostrarMensaje("mensajeSolicitud", "No se pudo registrar el prestamo: " + error.message, "error");
    } finally {
        btn.disabled = false;
    }
}

// 4. Devolucion de Material
function generarNuevaClaveDevolucion() {
    const clave = generarUuid();
    if ($("devClaveIdempotencia")) {
        $("devClaveIdempotencia").value = clave;
    }
    return clave;
}

function cambiarEstadoDevolucion() {
    const estado = $("devEstadoMaterial").value;
    const obs = $("devObservaciones");
    const sancion = $("devMontoSancion");

    if (estado === "DANADO_IRREPARABLE") {
        if (!obs.value || obs.value === "Material recibido en buen estado.") {
            obs.value = "Articulo con dano constatado en la recepcion.";
        }
        if (sancion) sancion.value = "250.00";
    } else if (estado === "REQUIERE_MANTENIMIENTO") {
        if (!obs.value || obs.value === "Material recibido en buen estado.") {
            obs.value = "Presenta desgaste, se envia a revision tecnica.";
        }
        if (sancion) sancion.value = "0.00";
    } else {
        if (obs.value.includes("dano") || obs.value.includes("desgaste")) {
            obs.value = "Material recibido en buen estado.";
        }
        if (sancion) sancion.value = "0.00";
    }
}

async function registrarDevolucion() {
    const clave = $("devClaveIdempotencia") ? $("devClaveIdempotencia").value.trim() : generarUuid();
    const idSolicitud = $("devIdSolicitud").value.trim();
    const codigoArticulo = $("devCodigoArticulo").value.trim();
    const idUsuario = parseInt($("devIdUsuario").value, 10);
    const estadoMaterial = $("devEstadoMaterial").value;
    const observaciones = $("devObservaciones").value.trim() || "Devolucion de material";
    const montoSancion = parseFloat($("devMontoSancion") ? $("devMontoSancion").value : "0.00") || 0.0;
    const btn = $("btnDevolver");

    const payload = {
        claveIdempotencia: clave,
        idSolicitud: idSolicitud,
        codigoArticulo: codigoArticulo,
        idUsuario: idUsuario,
        estadoMaterial: estadoMaterial,
        observaciones: observaciones,
        montoSancion: montoSancion,
        version: 0
    };

    try {
        btn.disabled = true;
        mostrarMensaje("mensajeDevolucion", "Guardando devolucion...", "info");

        const respuesta = await pedirJson(API_DEVOLUCIONES, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const textoSancion = respuesta.montoSancion > 0
            ? ` Sancion aplicada: $${Number(respuesta.montoSancion).toFixed(2)}.`
            : ` Sin sanciones aplicadas.`;

        mostrarMensaje(
            "mensajeDevolucion",
            `Devolucion guardada con exito. Folio: ${respuesta.folio}.${textoSancion}`,
            "exito"
        );

        // Prellenar campo del comprobante SOAP con el nuevo folio
        if ($("soapFolio")) {
            $("soapFolio").value = respuesta.folio;
        }

        // Recargar historial y datos del articulo
        cargarHistorialDevoluciones();
        consultarArticulo();

        // Generar nueva clave para la siguiente entrega
        generarNuevaClaveDevolucion();

    } catch (error) {
        mostrarMensaje("mensajeDevolucion", "Error al registrar la devolucion: " + error.message, "error");
    } finally {
        btn.disabled = false;
    }
}

// Cancelar una devolucion registrada
async function cancelarDevolucion(id, version) {
    if (!confirm(`¿Confirmas la cancelacion de la devolucion #${id}?`)) {
        return;
    }

    try {
        mostrarMensaje("mensajeHistorial", "Cancelando registro...", "info");

        const url = `${API_DEVOLUCIONES}/${id}/cancelar?version=${version}`;
        await pedirJson(url, { method: "PUT" });

        mostrarMensaje("mensajeHistorial", `Devolucion #${id} cancelada correctamente.`, "exito");
        cargarHistorialDevoluciones();
    } catch (error) {
        mostrarMensaje("mensajeHistorial", "No se pudo cancelar: " + error.message, "error");
    }
}

// 5. Historial de Devoluciones
async function cargarHistorialDevoluciones() {
    const cuerpo = $("cuerpoDevoluciones");
    if (!cuerpo) return;

    try {
        const lista = await pedirJson(API_DEVOLUCIONES);
        cuerpo.innerHTML = "";

        if (!lista || lista.length === 0) {
            cuerpo.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--muted);">No hay devoluciones registradas.</td></tr>`;
            return;
        }

        // Mostrar las mas recientes primero
        const ordenadas = [...lista].reverse();

        ordenadas.forEach(item => {
            const fila = document.createElement("tr");

            let badgeClass = "badge-disponible";
            let estadoTexto = "Buen Estado";
            if (item.estadoMaterial === "REQUIERE_MANTENIMIENTO") {
                badgeClass = "badge-mantenimiento";
                estadoTexto = "Mantenimiento";
            } else if (item.estadoMaterial === "DANADO_IRREPARABLE") {
                badgeClass = "badge-danado";
                estadoTexto = "Danado";
            }

            const fechaFormat = item.fechaDevolucion 
                ? new Date(item.fechaDevolucion).toLocaleString() 
                : "-";

            const sancionTexto = (item.montoSancion && item.montoSancion > 0)
                ? `<strong style="color: var(--danger);">$${Number(item.montoSancion).toFixed(2)}</strong>`
                : `$0.00`;

            const solicitudCorta = (item.idSolicitud && item.idSolicitud.length > 16)
                ? item.idSolicitud.substring(0, 13) + "..."
                : (item.idSolicitud || "-");

            const esCancelada = item.observaciones && item.observaciones.startsWith("[CANCELADA]");
            const accionCancelar = esCancelada
                ? `<span style="color: var(--muted); font-size: 0.8rem;">Cancelada</span>`
                : `<button type="button" class="btn btn-outline btn-sm" style="color: var(--danger);" onclick="cancelarDevolucion(${item.id}, ${item.version})">Cancelar</button>`;

            fila.innerHTML = `
                <td><strong>${item.folio || '-'}</strong></td>
                <td title="${item.idSolicitud || ''}">${solicitudCorta}</td>
                <td>${item.codigoArticulo || '-'}</td>
                <td>${item.idUsuario || '-'}</td>
                <td>${fechaFormat}</td>
                <td><span class="badge ${badgeClass}">${estadoTexto}</span></td>
                <td>${sancionTexto}</td>
                <td>${item.observaciones || '-'}</td>
                <td style="white-space: nowrap; display: flex; gap: 4px;">
                    ${accionCancelar}
                    <button type="button" class="btn btn-outline btn-sm" onclick="verComprobante('${item.folio}')">Comprobante</button>
                </td>
            `;
            cuerpo.appendChild(fila);
        });
    } catch (error) {
        cuerpo.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--danger);">Error al cargar historial: ${error.message}</td></tr>`;
    }
}

// 6. Consulta de Comprobante / Incidencias (SOAP)
function verComprobante(folio) {
    if ($("soapFolio")) {
        $("soapFolio").value = folio;
    }
    consultarComprobanteSoap();
}

async function consultarComprobanteSoap() {
    const folioInput = $("soapFolio");
    const visor = $("resultadoSoap");
    if (!folioInput || !folioInput.value.trim()) return;

    const folio = folioInput.value.trim();

    try {
        mostrarMensaje("mensajeSoap", `Consultando comprobante para el folio ${folio}...`, "info");
        if (visor) visor.style.display = "none";

        const url = `${API_DEVOLUCIONES}/transformacion-soap/${encodeURIComponent(folio)}`;
        const datos = await pedirJson(url);

        mostrarMensaje("mensajeSoap", "Comprobante consultado exitosamente.", "exito");

        if (visor) {
            $("soapResFolio").textContent = datos.folio || "-";
            $("soapResArticulo").textContent = datos.articulo || "-";
            $("soapResCondicion").textContent = datos.condicion || "-";
            $("soapResPenalizacion").textContent = datos.penalizacion ? `$${Number(datos.penalizacion).toFixed(2)}` : "$0.00";
            $("soapResReposicion").textContent = datos.requiereReposicion ? "Si, requiere compra de activo" : "No requerida";
            $("soapResDictamen").textContent = datos.dictamenTecnico || "-";
            $("soapResComentarios").textContent = datos.comentarios || "-";
            $("soapJsonRaw").textContent = JSON.stringify(datos, null, 2);
            visor.style.display = "block";
        }
    } catch (error) {
        mostrarMensaje("mensajeSoap", "No se pudo consultar el comprobante: " + error.message, "error");
        if (visor) visor.style.display = "none";
    }
}

// Alternar vista del JSON sin formato
function alternarJsonRaw() {
    const pre = $("soapJsonRaw");
    if (!pre) return;
    pre.style.display = pre.style.display === "none" ? "block" : "none";
}

// Inicializar al cargar el documento
window.addEventListener("DOMContentLoaded", () => {
    // Fecha de hoy por defecto en el prestamo
    const hoy = new Date().toISOString().split("T")[0];
    if ($("solFechaUso")) {
        $("solFechaUso").value = hoy;
    }

    // Inicializar clave de entrega
    generarNuevaClaveDevolucion();

    // Cargar consultas iniciales de ejemplo
    consultarArticulo();
    consultarUsuario();

    // Cargar historial de entregas
    cargarHistorialDevoluciones();
});
