import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/** Pruebas de integración sobre bases temporales, nunca sobre la agenda real. */
public class PruebasSQLite {
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) { throw new AssertionError(mensaje); }
    }

    private static GestorAcademicoController abrir(Path archivo) {
        GestorAcademicoController gestor = new GestorAcademicoController(archivo);
        comprobar(gestor.iniciarSesion("william", "UvG2026!"), "Falló el acceso.");
        return gestor;
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory("pruebas-gestor-sqlite-");
        Path archivo = carpeta.resolve("actividades.db");
        GestorAcademicoController gestor = abrir(archivo);
        Actividad tarea = gestor.registrarActividad("Tarea", "Café '); DROP TABLE actividades; --",
                "2026-11-20", "Alta", "Línea 1\nLínea 2 con ñ y tildes", true);
        LocalDateTime siguiente = tarea.getSiguienteRecordatorio();
        gestor.registrarActividad("Proyecto", "Entrega", "2026-11-21", "Media", "POO", false);
        gestor.registrarActividad("Examen", "Parcial", "2026-11-22", "Baja", "Matemática", false);
        gestor = abrir(archivo);
        comprobar(gestor.getActividades().size() == 3, "No recuperó todos los registros.");
        tarea = gestor.getActividades().get(0);
        comprobar(tarea instanceof Tarea && gestor.getActividades().get(1) instanceof Proyecto
                && gestor.getActividades().get(2) instanceof Examen, "Perdió los tipos.");
        comprobar(tarea.getNombre().contains("DROP TABLE"), "No conservó el texto literal.");
        comprobar(tarea.getDetalle().contains("\nLínea 2"), "Perdió texto multilínea.");
        comprobar(tarea.getSiguienteRecordatorio().equals(siguiente), "Reinició el aviso.");
        gestor.confirmarRecordatorio(tarea, siguiente.plusHours(30));
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.getSiguienteRecordatorio().equals(siguiente.plusHours(36)), "Perdió el horario.");
        gestor.editarActividad(tarea, "Tarea editada", "2026-11-23", "Media", "Nuevo detalle", true);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.getNombre().equals("Tarea editada"), "No guardó la edición.");
        gestor.marcarComoCompletada(tarea);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.isCompletada() && !tarea.isRecordatorioActivo(), "Falló completar.");
        gestor.reabrirActividad(tarea);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(!tarea.isCompletada() && !tarea.isRecordatorioActivo(), "Falló reabrir.");
        gestor.cambiarRecordatorio(tarea);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.isRecordatorioActivo(), "No guardó la activación.");

        GestorAcademicoController ventanaAntigua = abrir(archivo);
        gestor.eliminarActividad(tarea);
        boolean rechazo = false;
        try { ventanaAntigua.registrarActividad("Tarea", "Conflicto", "2026-11-20", "Alta", "", false); }
        catch (IllegalStateException esperado) { rechazo = true; }
        comprobar(rechazo && ventanaAntigua.getActividades().size() == 3, "No revirtió el conflicto.");
        comprobar(abrir(archivo).getActividades().size() == 2, "El conflicto sobrescribió datos.");

        // Obliga a fallar el INSERT después del DELETE para verificar el rollback SQL.
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + archivo); Statement s = c.createStatement()) {
            s.execute("CREATE TRIGGER prueba_fallo BEFORE INSERT ON actividades "
                    + "BEGIN SELECT RAISE(ABORT, 'fallo de prueba'); END");
        }
        rechazo = false;
        try { gestor.registrarActividad("Tarea", "No guardada", "2026-11-20", "Alta", "", false); }
        catch (IllegalStateException esperado) { rechazo = true; }
        comprobar(rechazo && gestor.getActividades().size() == 2, "No revirtió la memoria.");
        comprobar(abrir(archivo).getActividades().size() == 2, "El rollback SQL perdió registros.");

        Path migracion = Files.createDirectory(carpeta.resolve("migracion"));
        ArrayList<Actividad> anteriores = new ArrayList<>();
        anteriores.add(new Tarea("Desde XML", LocalDate.of(2026, 11, 20), "Alta", "Importada"));
        new RepositorioActividadesXML(migracion.resolve("actividades.xml")).guardar(anteriores);
        Path nueva = migracion.resolve("actividades.db");
        gestor = abrir(nueva);
        comprobar(gestor.getActividades().size() == 1, "Falló la migración.");
        gestor.eliminarActividad(gestor.getActividades().get(0));
        comprobar(abrir(nueva).getActividades().isEmpty(), "Reimportó un XML ya migrado.");
        comprobar(Files.exists(migracion.resolve("actividades.xml")), "Modificó el respaldo XML.");

        Path corrupto = carpeta.resolve("corrupto.db");
        Files.writeString(corrupto, "esto no es SQLite");
        rechazo = false;
        try { abrir(corrupto); } catch (IllegalStateException esperado) { rechazo = true; }
        comprobar(rechazo && Files.readString(corrupto).equals("esto no es SQLite"), "Ignoró o dañó datos corruptos.");
        System.out.println("Pruebas SQLite correctas: CRUD, avisos, rollback, conflicto, migración y archivo dañado.");
    }
}
