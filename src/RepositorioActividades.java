import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Guarda la agenda personal en SQLite mediante JDBC y transacciones. */
public class RepositorioActividades {
    private final Path archivo;
    private long revision = -1;

    public RepositorioActividades(Path archivo) {
        if (archivo == null) { throw new IllegalArgumentException("Selecciona un archivo de base de datos."); }
        this.archivo = archivo.toAbsolutePath().normalize();
    }

    private Connection conectar() throws SQLException, IOException {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException | LinkageError error) {
            throw new IllegalStateException("Falta el controlador SQLite o una dependencia. "
                    + "Coloca sqlite-jdbc y sus dependencias SLF4J en lib y vuelve a abrir el proyecto.", error);
        }
        Files.createDirectories(archivo.getParent());
        Connection conexion = DriverManager.getConnection("jdbc:sqlite:" + archivo);
        try (Statement instruccion = conexion.createStatement()) {
            instruccion.execute("PRAGMA busy_timeout = 5000");
            instruccion.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException error) { conexion.close(); throw error; }
        return conexion;
    }

    private void preparar(Connection conexion) throws SQLException {
        try (Statement sql = conexion.createStatement()) {
            try (ResultSet version = sql.executeQuery("PRAGMA user_version")) {
                if (version.next() && version.getInt(1) > 1) {
                    throw new SQLException("La base de datos pertenece a una versión posterior.");
                }
            }
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS agenda_control ("
                    + "id INTEGER PRIMARY KEY CHECK (id = 1), "
                    + "revision INTEGER NOT NULL DEFAULT 0, "
                    + "migracion_xml INTEGER NOT NULL DEFAULT 0 CHECK (migracion_xml IN (0,1)))");
            sql.executeUpdate("INSERT OR IGNORE INTO agenda_control (id) VALUES (1)");
            sql.executeUpdate("CREATE TABLE IF NOT EXISTS actividades ("
                    + "id INTEGER PRIMARY KEY, "
                    + "tipo TEXT NOT NULL CHECK (tipo IN ('Tarea','Proyecto','Examen')), "
                    + "nombre TEXT NOT NULL CHECK (length(trim(nombre)) > 0), "
                    + "fecha TEXT NOT NULL, "
                    + "prioridad TEXT NOT NULL CHECK (prioridad IN ('Alta','Media','Baja')), "
                    + "detalle TEXT NOT NULL, "
                    + "completada INTEGER NOT NULL CHECK (completada IN (0,1)), "
                    + "recordatorio_activo INTEGER NOT NULL CHECK (recordatorio_activo IN (0,1)), "
                    + "siguiente_recordatorio TEXT NOT NULL, "
                    + "CHECK (completada = 0 OR recordatorio_activo = 0))");
            sql.execute("PRAGMA user_version = 1");
        }
    }

    /** Abre una instantánea consistente y reconstruye los tipos concretos. */
    public ArrayList<Actividad> cargar() {
        try (Connection conexion = conectar()) {
            preparar(conexion);
            conexion.setAutoCommit(false);
            try {
                migrarXML(conexion);
                ArrayList<Actividad> actividades = leer(conexion);
                long encontrada = leerRevision(conexion);
                conexion.commit();
                revision = encontrada;
                return actividades;
            } catch (SQLException | RuntimeException error) {
                deshacer(conexion, error); throw error;
            }
        } catch (SQLException | IOException error) {
            throw new IllegalStateException("No se pudo abrir la base de datos " + archivo
                    + ". Revisa la ruta, los permisos y que el archivo sea SQLite.", error);
        }
    }

    private ArrayList<Actividad> leer(Connection conexion) throws SQLException {
        ArrayList<Actividad> resultado = new ArrayList<>();
        try (Statement sql = conexion.createStatement();
             ResultSet filas = sql.executeQuery("SELECT * FROM actividades ORDER BY id")) {
            while (filas.next()) {
                LocalDate fecha = LocalDate.parse(filas.getString("fecha"));
                String nombre = filas.getString("nombre");
                String prioridad = filas.getString("prioridad");
                String detalle = filas.getString("detalle");
                Actividad actividad;
                String tipo = filas.getString("tipo");
                if ("Tarea".equals(tipo)) { actividad = new Tarea(nombre, fecha, prioridad, detalle); }
                else if ("Proyecto".equals(tipo)) { actividad = new Proyecto(nombre, fecha, prioridad, detalle); }
                else if ("Examen".equals(tipo)) { actividad = new Examen(nombre, fecha, prioridad, detalle); }
                else { throw new IllegalStateException("Tipo de actividad desconocido en la base de datos."); }
                actividad.restaurarEstado(filas.getInt("completada") == 1,
                        filas.getInt("recordatorio_activo") == 1,
                        LocalDateTime.parse(filas.getString("siguiente_recordatorio")));
                resultado.add(actividad);
            }
        }
        return resultado;
    }

    private long leerRevision(Connection conexion) throws SQLException {
        try (Statement sql = conexion.createStatement();
             ResultSet fila = sql.executeQuery("SELECT revision FROM agenda_control WHERE id = 1")) {
            if (!fila.next()) { throw new SQLException("Falta el control de la agenda."); }
            return fila.getLong(1);
        }
    }

    /** Importa el XML una sola vez y conserva intacto el archivo original. */
    private void migrarXML(Connection conexion) throws SQLException {
        boolean pendiente;
        int cantidad;
        try (Statement sql = conexion.createStatement();
             ResultSet fila = sql.executeQuery("SELECT migracion_xml FROM agenda_control WHERE id = 1")) {
            pendiente = fila.next() && fila.getInt(1) == 0;
        }
        if (!pendiente) { return; }
        try (Statement sql = conexion.createStatement();
             ResultSet fila = sql.executeQuery("SELECT count(*) FROM actividades")) {
            cantidad = fila.next() ? fila.getInt(1) : 0;
        }
        Path anterior = archivo.resolveSibling("actividades.xml");
        if (cantidad == 0 && Files.exists(anterior)) {
            ArrayList<Actividad> importadas = new RepositorioActividadesXML(anterior).cargar();
            escribir(conexion, importadas);
            try (Statement sql = conexion.createStatement()) {
                sql.executeUpdate("UPDATE agenda_control SET revision = revision + 1 WHERE id = 1");
            }
        }
        try (Statement sql = conexion.createStatement()) {
            sql.executeUpdate("UPDATE agenda_control SET migracion_xml = 1 WHERE id = 1");
        }
    }

    /** Confirma la lista completa de una agenda pequeña o revierte toda la operación. */
    public void guardar(List<Actividad> actividades) {
        if (revision < 0) { throw new IllegalStateException("Primero carga la agenda antes de guardar."); }
        try (Connection conexion = conectar()) {
            conexion.setAutoCommit(false);
            try {
                long actual = leerRevision(conexion);
                if (actual != revision) {
                    throw new IllegalStateException("Otra ventana cambió la agenda. "
                            + "Cierra esta ventana y vuelve a abrirla para recuperar los cambios.");
                }
                escribir(conexion, actividades);
                try (PreparedStatement sql = conexion.prepareStatement(
                        "UPDATE agenda_control SET revision = revision + 1 WHERE id = 1 AND revision = ?")) {
                    sql.setLong(1, revision);
                    if (sql.executeUpdate() != 1) { throw new SQLException("La agenda cambió mientras se guardaba."); }
                }
                conexion.commit();
                revision = actual + 1;
            } catch (SQLException | RuntimeException error) {
                deshacer(conexion, error); throw error;
            }
        } catch (SQLException | IOException error) {
            throw new IllegalStateException("No se pudo guardar en SQLite. El cambio no se aplicó. "
                    + "Revisa los permisos, el espacio disponible y otras ventanas de la agenda.", error);
        }
    }

    private void escribir(Connection conexion, List<Actividad> actividades) throws SQLException {
        try (Statement sql = conexion.createStatement()) { sql.executeUpdate("DELETE FROM actividades"); }
        String consulta = "INSERT INTO actividades "
                + "(id,tipo,nombre,fecha,prioridad,detalle,completada,recordatorio_activo,siguiente_recordatorio) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement sql = conexion.prepareStatement(consulta)) {
            for (int i = 0; i < actividades.size(); i++) {
                Actividad a = actividades.get(i);
                sql.setInt(1, i + 1);
                sql.setString(2, a.getTipo());
                sql.setString(3, a.getNombre());
                sql.setString(4, a.getFechaEntrega().toString());
                sql.setString(5, a.getPrioridad());
                sql.setString(6, a.getDetalle());
                sql.setInt(7, a.isCompletada() ? 1 : 0);
                sql.setInt(8, a.isRecordatorioActivo() ? 1 : 0);
                sql.setString(9, a.getSiguienteRecordatorio().toString());
                sql.addBatch();
            }
            sql.executeBatch();
        }
    }

    private void deshacer(Connection conexion, Exception original) {
        try { conexion.rollback(); }
        catch (SQLException error) { original.addSuppressed(error); }
    }
}
