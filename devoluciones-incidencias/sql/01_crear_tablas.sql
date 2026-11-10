BEGIN;

CREATE TABLE public.devoluciones (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    folio VARCHAR(36) NOT NULL UNIQUE,
    id_solicitud VARCHAR(64) NOT NULL,
    codigo_articulo VARCHAR(30) NOT NULL,
    id_usuario BIGINT NOT NULL,
    fecha_devolucion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_material VARCHAR(30) NOT NULL,
    observaciones VARCHAR(500) NOT NULL,
    monto_sancion NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_folio_formato
        CHECK (length(trim(folio)) >= 10),

    CONSTRAINT chk_id_solicitud_no_vacio
        CHECK (length(trim(id_solicitud)) > 0),

    CONSTRAINT chk_codigo_articulo_no_vacio
        CHECK (length(trim(codigo_articulo)) > 0),

    CONSTRAINT chk_estado_material
        CHECK (estado_material IN ('BUEN_ESTADO', 'REQUIERE_MANTENIMIENTO', 'DANADO_IRREPARABLE')),

    CONSTRAINT chk_monto_sancion_no_negativo
        CHECK (monto_sancion >= 0.00),

    CONSTRAINT chk_version_no_negativa
        CHECK (version >= 0)
);

CREATE TABLE public.idempotencia_devoluciones (
    clave_idempotencia UUID PRIMARY KEY,
    huella_solicitud VARCHAR(64) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    codigo_estado_http INTEGER NOT NULL,
    respuesta_json TEXT,
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_estado_idempotencia
        CHECK (estado IN ('PROCESANDO', 'COMPLETADO', 'FALLIDO'))
);

COMMIT;

