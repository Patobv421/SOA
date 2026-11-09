BEGIN;

CREATE TABLE public.articulos (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL, 
    deporte VARCHAR(60) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    cantidad_buen_estado INTEGER NOT NULL,
    cantidad_mantenimiento INTEGER NOT NULL,
    cantidad_danada INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_codigo_formato
        CHECK (codigo ~ '^[A-Z0-9-]{1,30}$'),

    CONSTRAINT chk_nombre_no_vacio
        CHECK (length(trim(nombre)) > 0),

    CONSTRAINT chk_deporte_no_vacio
        CHECK (length(trim(nombre)) > 0),

    CONSTRAINT chk_cantidad_buen_estado
        CHECK (cantidad_buen_estado BETWEEN 0 AND 1000000),

    CONSTRAINT chk_cantidad_mantenimiento
        CHECK (cantidad_mantenimiento BETWEEN 0 AND 1000000),

    CONSTRAINT chk_cantidad_danada
        CHECK (cantidad_danada BETWEEN 0 AND 1000000),

    CONSTRAINT chk_version
        CHECK (version >= 0)
);

COMMIT;