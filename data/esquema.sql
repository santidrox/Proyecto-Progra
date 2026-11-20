CREATE TABLE IF NOT EXISTS agenda_control (id INTEGER PRIMARY KEY CHECK (id = 1), revision INTEGER NOT NULL DEFAULT 0, migracion_xml INTEGER NOT NULL DEFAULT 0 CHECK (migracion_xml IN (0,1)));

CREATE TABLE IF NOT EXISTS actividades (id INTEGER PRIMARY KEY, tipo TEXT NOT NULL CHECK (tipo IN ('Tarea','Proyecto','Examen')), nombre TEXT NOT NULL CHECK (length(trim(nombre)) > 0), fecha TEXT NOT NULL, prioridad TEXT NOT NULL CHECK (prioridad IN ('Alta','Media','Baja')), detalle TEXT NOT NULL, completada INTEGER NOT NULL CHECK (completada IN (0,1)), recordatorio_activo INTEGER NOT NULL CHECK (recordatorio_activo IN (0,1)), siguiente_recordatorio TEXT NOT NULL, CHECK (completada = 0 OR recordatorio_activo = 0));

INSERT OR IGNORE INTO agenda_control (id) VALUES (1);
PRAGMA user_version = 1;
