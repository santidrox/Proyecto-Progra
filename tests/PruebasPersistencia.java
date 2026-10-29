import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class PruebasPersistencia {
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) { throw new AssertionError(mensaje); }
    }

    private static GestorAcademicoController abrir(Path archivo) {
        GestorAcademicoController gestor = new GestorAcademicoController(archivo);
        comprobar(gestor.iniciarSesion("william", "UvG2026!"), "Falló el acceso.");
        return gestor;
    }

    public static void main(String[] args) throws Exception {
        Path carpeta = Files.createTempDirectory(Path.of("..").toAbsolutePath(), "validacion-agenda-");
        Path archivo = carpeta.resolve("actividades.xml");
        GestorAcademicoController gestor = abrir(archivo);
        Actividad tarea = gestor.registrarActividad("Tarea", "Café & <Java>", "2026-11-01", "Alta",
                "Primera línea\nSegunda línea con áéíóú y ñ", true);
        LocalDateTime siguiente = tarea.getSiguienteRecordatorio();
        gestor.registrarActividad("Proyecto", "Entrega", "2026-11-02", "Media", "Descripción", false);
        gestor.registrarActividad("Examen", "Parcial", "2026-11-03", "Baja", "Matemática", false);

        gestor = abrir(archivo);
        comprobar(gestor.getActividades().size() == 3, "No recuperó todos los tipos.");
        tarea = gestor.getActividades().get(0);
        comprobar(tarea instanceof Tarea, "No reconstruyó el tipo Tarea.");
        comprobar(gestor.getActividades().get(1) instanceof Proyecto, "No reconstruyó Proyecto.");
        comprobar(gestor.getActividades().get(2) instanceof Examen, "No reconstruyó Examen.");
        comprobar(tarea.getNombre().equals("Café & <Java>"), "Perdió caracteres especiales.");
        comprobar(tarea.getDetalle().contains("\nSegunda"), "Perdió saltos de línea.");
        comprobar(tarea.getSiguienteRecordatorio().equals(siguiente), "Reinició el horario al cargar.");
        comprobar(tarea.isRecordatorioActivo(), "Perdió la activación del aviso.");
        comprobar(gestor.obtenerRecordatoriosPendientes(siguiente).contains(tarea), "No detectó el aviso recuperado.");
        gestor.confirmarRecordatorio(tarea, siguiente.plusHours(30));
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.getSiguienteRecordatorio().equals(siguiente.plusHours(36)), "No conservó el nuevo intervalo.");
        gestor.editarActividad(tarea, "Tarea corregida", "2026-11-04", "Media", "Detalle actualizado", true);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.getNombre().equals("Tarea corregida"), "No guardó la edición.");
        comprobar(tarea.getSiguienteRecordatorio().equals(siguiente.plusHours(36)), "Editar alteró el horario.");
        gestor.marcarComoCompletada(tarea);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.isCompletada() && !tarea.isRecordatorioActivo(), "No guardó la finalización.");
        gestor.reabrirActividad(tarea);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(!tarea.isCompletada() && !tarea.isRecordatorioActivo(), "No guardó la reapertura.");
        gestor.cambiarRecordatorio(tarea);
        gestor = abrir(archivo);
        tarea = gestor.getActividades().get(0);
        comprobar(tarea.isRecordatorioActivo(), "No guardó el cambio de aviso.");
        gestor.eliminarActividad(tarea);
        comprobar(abrir(archivo).getActividades().size() == 2, "No guardó la eliminación.");
        comprobar(Files.isRegularFile(carpeta.resolve("actividades.xml.bak")), "No generó copia anterior.");

        Path corrupto = carpeta.resolve("corrupto.xml");
        Files.writeString(corrupto, "archivo dañado");
        boolean rechazo = false;
        try { abrir(corrupto); } catch (IllegalStateException esperado) { rechazo = true; }
        comprobar(rechazo, "Ignoró datos corruptos.");
        comprobar(Files.readString(corrupto).equals("archivo dañado"), "Sobrescribió el archivo corrupto.");

        Path padreArchivo = carpeta.resolve("no-es-carpeta");
        Files.writeString(padreArchivo, "contenido");
        GestorAcademicoController sinPermisoDeRuta = abrir(padreArchivo.resolve("agenda.xml"));
        rechazo = false;
        try {
            sinPermisoDeRuta.registrarActividad("Tarea", "No guardada", "2026-11-01", "Alta", "", false);
        } catch (IllegalStateException esperado) { rechazo = true; }
        comprobar(rechazo && sinPermisoDeRuta.getActividades().isEmpty(), "No deshizo el registro fallido.");
        System.out.println("Pruebas de persistencia, edición y recuperación correctas.");
    }
}
