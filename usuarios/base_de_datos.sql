-- Ejecutar con el usuario postgres (pgAdmin o psql)
-- Base y usuario propios del servicio de Usuarios. Ningun otro servicio tiene permisos aqui.

CREATE USER usuarios_user WITH PASSWORD 'usuarios123';

CREATE DATABASE usuarios_db OWNER usuarios_user;

REVOKE ALL ON DATABASE usuarios_db FROM PUBLIC;
