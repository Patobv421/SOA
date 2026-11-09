// Consumidor Web - Gestion de Material Deportivo y Devoluciones

const API_INVENTARIO = "http://localhost:9000/api/articulos";
const API_INVENTARIO_FALLBACK = "http://localhost:9010/api/devoluciones/inventario";

const API_USUARIOS = "http://localhost:8083/api/usuarios";
const API_USUARIOS_FALLBACK = "http://localhost:9010/api/devoluciones/usuarios";

const API_SOLICITUDES = "http://localhost:8082/api/solicitudes";
const API_DEVOLUCIONES = "http://localhost:9010/api/devoluciones";

const CLAVE_STORAGE_SOLICITUD = "soa.solicitud.operacion";

function elemento(id) {
    return document.getElementById(id);
}

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

function mostrarMensaje(idElemento, texto, esError = false) {
    const el = elemento(idElemento);
    if (!el) return;
    el.style.display = "block";
    el.className = "mensaje-box " + (esError ? "error" : "exito");
    el.textContent = texto;
}

async function solicitarJson(url, opciones = {}) {
    const controlador = new AbortController();
    const temporizador = setTimeout(() => controlador.abort(), 6000);

    try {
        const respuesta = await fetch(url, {
            ...opciones,
            headers: {
                "Accept": "application/json",
                ...(opciones.headers || {})
            },
            signal: controlador.signal
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
            let detalle = "Error HTTP " + respuesta.status;
            if (datos) {
                if (datos.mensaje) detalle = datos.mensaje;
                else if (datos.message) detalle = datos.message;
                else if (datos.error) detalle = datos.error;
            }
            const err = new Error(detalle);
            err.status = respuesta.status;
            err.datos = datos;
            throw err;
        }

        return datos;
    } catch (error) {
        if (error.name === "AbortError") {
            throw new Error("Tiempo de espera agotado al comunicar con el servicio.");
        }
        throw error;
    } finally {
        clearTimeout(temporizador);
    }
}

async function solicitarConFallback(urlDirecta, urlFallback, opciones = {}) {
    try {
        return await solicitarJson(urlDirecta, opciones);
    } catch (err) {
        if (urlFallback && (err.name === "TypeError" || !err.status || (err.message && err.message.includes("fetch")))) {
            return await solicitarJson(urlFallback, opciones);
        }
        throw err;
    }
}

// 1. Consultar Articulo
async function consultarArticulo() {
    const id = elemento("articuloId").value;
    const detalle = elemento("detalleArticulo");
    if (!id) return;

    try {
        mostrarMensaje("mensajeArticulo", "Consultando catalogo...");
        elemento("mensajeArticulo").className = "mensaje-box info";

        const urlDirecta = `${API_INVENTARIO}/${id}`;
        const urlFallback = `${API_INVENTARIO_FALLBACK}/${id}`;
        const articulo = await solicitarConFallback(urlDirecta, urlFallback);

        elemento("artCodigo").textContent = articulo.codigo;
        elemento("artNombre").textContent = articulo.nombre;
        elemento("artDeporte").textContent = articulo.deporte || "-";
        elemento("artBuenEstado").innerHTML = `<span class="badge badge-disponible">${articulo.cantidadBuenEstado} unidades</span>`;
        elemento("artMantenimiento").innerHTML = `<span class="badge badge-mantenimiento">${articulo.cantidadMantenimiento}</span>`;
        elemento("artDanadas").innerHTML = `<span class="badge badge-danado">${articulo.cantidadDanada}</span>`;

        // Prellenar formularios relacionados
        elemento("solIdArticulo").value = articulo.id;
        elemento("devCodigoArticulo").value = articulo.codigo;

        detalle.style.display = "grid";
        mostrarMensaje("mensajeArticulo", `Articulo [${articulo.codigo}] cargado exitosamente.`);
    } catch (error) {
        if (detalle) detalle.style.display = "none";
        mostrarMensaje("mensajeArticulo", error.message || "No fue posible consultar el articulo.", true);
    }
}

async function cargarCatalogoCompleto() {
    const contenedor = elemento("contenedorTablaArticulos");
    const cuerpo = elemento("cuerpoTablaArticulos");

    try {
        mostrarMensaje("mensajeArticulo", "Cargando lista de articulos...");
        elemento("mensajeArticulo").className = "mensaje-box info";

        const articulos = await solicitarConFallback(API_INVENTARIO, API_INVENTARIO_FALLBACK);
        cuerpo.innerHTML = "";

        if (!articulos || articulos.length === 0) {
            cuerpo.innerHTML = `<tr><td colspan="8" style="text-align:center;">No hay articulos en el catalogo.</td></tr>`;
        } else {
            articulos.forEach(art => {
                const tr = document.createElement("tr");
                tr.innerHTML = `
                    <td>${art.id}</td>
                    <td><strong>${art.codigo}</strong></td>
                    <td>${art.nombre}</td>
                    <td>${art.deporte || '-'}</td>
                    <td><span class="badge badge-disponible">${art.cantidadBuenEstado}</span></td>
                    <td><span class="badge badge-mantenimiento">${art.cantidadMantenimiento}</span></td>
                    <td><span class="badge badge-danado">${art.cantidadDanada}</span></td>
                    <td>
                        <button type="button" class="btn btn-outline" style="padding: 4px 8px; font-size: 0.8rem;" onclick="seleccionarArticulo(${art.id})">
                            Seleccionar
                        </button>
                    </td>
                `;
                cuerpo.appendChild(tr);
            });
        }

        contenedor.style.display = "block";
        mostrarMensaje("mensajeArticulo", `Se cargaron ${articulos.length} articulos del inventario.`);
    } catch (error) {
        mostrarMensaje("mensajeArticulo", "Error al cargar el catalogo: " + error.message, true);
    }
}

function seleccionarArticulo(id) {
    elemento("articuloId").value = id;
    consultarArticulo();
}

// 2. Consultar Usuario
async function consultarUsuario() {
    const id = elemento("usuarioId").value;
    const detalle = elemento("detalleUsuario");
    if (!id) return;

    try {
        mostrarMensaje("mensajeUsuario", "Consultando usuario...");
        elemento("mensajeUsuario").className = "mensaje-box info";

        const urlDirecta = `${API_USUARIOS}/${id}`;
        const urlFallback = `${API_USUARIOS_FALLBACK}/${id}`;
        const usuario = await solicitarConFallback(urlDirecta, urlFallback);

        elemento("usrMatricula").textContent = usuario.matricula;
        elemento("usrNombre").textContent = usuario.nombre;
        elemento("usrCorreo").textContent = usuario.correo || "-";
        elemento("usrTipo").textContent = usuario.tipo;

        const estadoBadge = usuario.estado === "ACTIVO"
            ? '<span class="badge badge-disponible">ACTIVO</span>'
            : '<span class="badge badge-danado">' + usuario.estado + '</span>';
        elemento("usrEstado").innerHTML = estadoBadge;

        const limiteTexto = (usuario.limiteSolicitudes !== undefined) 
            ? `${usuario.limiteSolicitudes} solicitudes max.`
            : (usuario.tipo === "DOCENTE" ? "5 solicitudes max." : "3 solicitudes max.");
        elemento("usrLimite").textContent = limiteTexto;

        // Prellenar formularios relacionados
        elemento("solIdUsuario").value = usuario.id;
        elemento("devIdUsuario").value = usuario.id;

        detalle.style.display = "grid";
        mostrarMensaje("mensajeUsuario", `Usuario ${usuario.nombre} verificado.`);
    } catch (error) {
        if (detalle) detalle.style.display = "none";
        mostrarMensaje("mensajeUsuario", error.message || "No fue posible consultar el usuario.", true);
    }
}

// 3. Solicitar Prestamo
function inicializarUuidSolicitud() {
    let uuid = localStorage.getItem(CLAVE_STORAGE_SOLICITUD);
    if (!uuid) {
        uuid = generarUuid();
        localStorage.setItem(CLAVE_STORAGE_SOLICITUD, uuid);
    }
    const banner = elemento("bannerSolicitudPendiente");
    if (banner) {
        banner.style.display = "flex";
        elemento("textoSolicitudPendiente").textContent = "Identificador de operacion: " + uuid.substring(0, 8) + "...";
    }
    return uuid;
}

function generarNuevoIdentificadorOperacion() {
    const nuevoUuid = generarUuid();
    localStorage.setItem(CLAVE_STORAGE_SOLICITUD, nuevoUuid);
    const banner = elemento("bannerSolicitudPendiente");
    if (banner) {
        elemento("textoSolicitudPendiente").textContent = "Nuevo identificador: " + nuevoUuid.substring(0, 8) + "...";
    }
    mostrarMensaje("mensajeSolicitud", "Identificador renovado para una nueva solicitud.");
    elemento("mensajeSolicitud").className = "mensaje-box info";
}

async function registrarSolicitud() {
    const idArticulo = parseInt(elemento("solIdArticulo").value, 10);
    const idUsuario = parseInt(elemento("solIdUsuario").value, 10);
    const cantidad = parseInt(elemento("solCantidad").value, 10);
    const fechaUso = elemento("solFechaUso").value;
    const btn = elemento("btnSolicitar");

    const idOperacion = localStorage.getItem(CLAVE_STORAGE_SOLICITUD) || generarUuid();

    const payload = {
        idOperacion: idOperacion,
        idArticulo: idArticulo,
        idUsuario: idUsuario,
        cantidad: cantidad,
        fechaUso: fechaUso
    };

    try {
        btn.disabled = true;
        mostrarMensaje("mensajeSolicitud", "Registrando solicitud de prestamo...");
        elemento("mensajeSolicitud").className = "mensaje-box info";

        const respuesta = await solicitarJson(API_SOLICITUDES, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        // Prellenar la devolucion con el id de solicitud devuelto
        if (respuesta && respuesta.idSolicitud) {
            elemento("devIdSolicitud").value = respuesta.idSolicitud;
        } else {
            elemento("devIdSolicitud").value = idOperacion;
        }

        mostrarMensaje(
            "mensajeSolicitud",
            `Solicitud registrada con exito. Estado: ${respuesta.estado || 'CONFIRMADA'}. Listo para devolucion.`
        );

        // Generar nuevo UUID para el siguiente prestamo
        generarNuevoIdentificadorOperacion();
        
        // Actualizar datos del articulo para ver el descuento en inventario
        consultarArticulo();
    } catch (error) {
        mostrarMensaje("mensajeSolicitud", "No se pudo registrar la solicitud: " + error.message, true);
    } finally {
        btn.disabled = false;
    }
}

// 4. Registrar Devolucion
function cambioEstadoDevolucion() {
    const estado = elemento("devEstadoMaterial").value;
    const obs = elemento("devObservaciones");
    if (estado === "DANADO_IRREPARABLE") {
        if (!obs.value || obs.value === "Material recibido en condiciones optimas") {
            obs.value = "Articulo con dano irreparable constatado en recepcion.";
        }
    } else if (estado === "REQUIERE_MANTENIMIENTO") {
        if (!obs.value || obs.value === "Material recibido en condiciones optimas") {
            obs.value = "Presenta desgaste que requiere revision tecnica.";
        }
    } else {
        if (obs.value.includes("dano") || obs.value.includes("desgaste")) {
            obs.value = "Material recibido en condiciones optimas";
        }
    }
}

async function registrarDevolucion() {
    const idSolicitud = elemento("devIdSolicitud").value.trim();
    const codigoArticulo = elemento("devCodigoArticulo").value.trim();
    const idUsuario = parseInt(elemento("devIdUsuario").value, 10);
    const estadoMaterial = elemento("devEstadoMaterial").value;
    const observaciones = elemento("devObservaciones").value.trim() || "Devolucion ordinaria de material";
    const btn = elemento("btnDevolver");

    const payload = {
        idSolicitud: idSolicitud,
        codigoArticulo: codigoArticulo,
        idUsuario: idUsuario,
        estadoMaterial: estadoMaterial,
        observaciones: observaciones,
        version: 0
    };

    try {
        btn.disabled = true;
        mostrarMensaje("mensajeDevolucion", "Procesando devolucion en Servicio 4...");
        elemento("mensajeDevolucion").className = "mensaje-box info";

        const respuesta = await solicitarJson(API_DEVOLUCIONES, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const sancionTexto = respuesta.montoSancion > 0
            ? ` Sancion aplicada: $${respuesta.montoSancion.toFixed(2)}.`
            : ` Sin sanciones aplicadas.`;

        mostrarMensaje(
            "mensajeDevolucion",
            `Devolucion exitosa. Folio: ${respuesta.folio}.${sancionTexto} Inventario actualizado.`
        );

        // Recargar el historial y el articulo
        cargarHistorialDevoluciones();
        consultarArticulo();
    } catch (error) {
        mostrarMensaje("mensajeDevolucion", "Error al registrar la devolucion: " + error.message, true);
    } finally {
        btn.disabled = false;
    }
}

// 5. Historial de Devoluciones
async function cargarHistorialDevoluciones() {
    const cuerpo = elemento("cuerpoDevoluciones");
    if (!cuerpo) return;

    try {
        const lista = await solicitarJson(API_DEVOLUCIONES);
        cuerpo.innerHTML = "";

        if (!lista || lista.length === 0) {
            cuerpo.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--muted);">No hay devoluciones registradas.</td></tr>`;
            return;
        }

        // Mostrar las devoluciones mas recientes primero
        const ordenadas = [...lista].reverse();

        ordenadas.forEach(item => {
            const tr = document.createElement("tr");

            let badgeClass = "badge-disponible";
            let estadoTexto = "Buen Estado";
            if (item.estadoMaterial === "REQUIERE_MANTENIMIENTO") {
                badgeClass = "badge-mantenimiento";
                estadoTexto = "Mantenimiento";
            } else if (item.estadoMaterial === "DANADO_IRREPARABLE") {
                badgeClass = "badge-danado";
                estadoTexto = "Danado Irreparable";
            }

            const fechaFormat = item.fechaDevolucion 
                ? new Date(item.fechaDevolucion).toLocaleString() 
                : "-";

            const sancionTexto = (item.montoSancion && item.montoSancion > 0)
                ? `<strong style="color: var(--danger);">$${Number(item.montoSancion).toFixed(2)}</strong>`
                : `$0.00`;

            const solicitudCorta = (item.idSolicitud && item.idSolicitud.length > 18)
                ? item.idSolicitud.substring(0, 15) + "..."
                : (item.idSolicitud || "-");

            tr.innerHTML = `
                <td><strong>${item.folio || '-'}</strong></td>
                <td title="${item.idSolicitud || ''}">${solicitudCorta}</td>
                <td>${item.codigoArticulo || '-'}</td>
                <td>${item.idUsuario || '-'}</td>
                <td>${fechaFormat}</td>
                <td><span class="badge ${badgeClass}">${estadoTexto}</span></td>
                <td>${sancionTexto}</td>
                <td>${item.observaciones || '-'}</td>
            `;
            cuerpo.appendChild(tr);
        });
    } catch (error) {
        cuerpo.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--danger);">Error al cargar historial: ${error.message}</td></tr>`;
    }
}

// Inicializacion al cargar la pagina
window.addEventListener("DOMContentLoaded", () => {
    // Establecer fecha de hoy en el formulario de solicitud
    const hoy = new Date().toISOString().split("T")[0];
    if (elemento("solFechaUso")) {
        elemento("solFechaUso").value = hoy;
    }

    // Inicializar identificador de operacion
    inicializarUuidSolicitud();

    // Consultar automaticamente articulo 1 y usuario 1
    consultarArticulo();
    consultarUsuario();

    // Cargar historial de devoluciones
    cargarHistorialDevoluciones();
});
