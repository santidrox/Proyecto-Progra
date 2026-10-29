import java.time.LocalDateTime;

public class Pruebas {
    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) { throw new AssertionError(mensaje); }
    }

    private static void debeFallar(Runnable accion) {
        try { accion.run(); }
        catch (IllegalArgumentException | IllegalStateException esperado) { return; }
        throw new AssertionError("La operación debía ser rechazada.");
    }

    public static void main(String[] args) {
        GestorAcademicoController gestor = new GestorAcademicoController();
        debeFallar(() -> gestor.registrarActividad("Tarea", "Sin sesión", "2026-10-20", "Alta", "", false));
        comprobar(!gestor.iniciarSesion("william", "incorrecta"), "Aceptó una clave incorrecta.");
        comprobar(gestor.iniciarSesion("william", "UvG2026!"), "Rechazó el usuario de prueba.");
        comprobar(gestor.getIntentosRestantes() == 3, "No reinició los intentos.");
        Actividad tarea = gestor.registrarActividad("Tarea", "Progra", "2026-10-20", "Alta", "Ejercicio", true);
        Actividad proyecto = gestor.registrarActividad("Proyecto", "Entrega", "2026-10-21", "Media", "", false);
        Actividad examen = gestor.registrarActividad("Examen", "Parcial", "2026-10-22", "Baja", "Matemática", false);
        comprobar(tarea instanceof Tarea && proyecto instanceof Proyecto && examen instanceof Examen,
                "Los tipos de actividad no coinciden.");
        comprobar(gestor.getActividades().size() == 3, "Las actividades no comparten la lista.");
        comprobar(gestor.obtenerResumen(null)[2] == 0, "El progreso inicial no es cero.");
        comprobar(gestor.obtenerResumen(java.time.LocalDate.of(2026, 10, 20))[1] == 1, "El resumen diario incluye otras fechas.");
        comprobar(gestor.obtenerResumen(java.time.LocalDate.of(2026, 1, 1))[2] == 0, "El día sin actividades falla.");
        comprobar(gestor.listar("Todas", "Examen", "", "Fecha", null).equals(java.util.List.of(examen)), "Falló el filtro de tipo.");
        comprobar(gestor.listar("Todas", "Todos", "MATEMÁTICA", "Fecha", null).equals(java.util.List.of(examen)), "Falló la búsqueda.");
        comprobar(gestor.listar("Todas", "Todos", "", "Fecha", java.time.LocalDate.of(2026, 10, 21))
                .equals(java.util.List.of(proyecto)), "Falló el filtro del calendario.");
        debeFallar(() -> gestor.registrarActividad("Tarea", "", "2026-10-20", "Alta", "", false));
        debeFallar(() -> gestor.registrarActividad("Tarea", "Fecha inválida", "2026-02-30", "Alta", "", false));
        debeFallar(() -> gestor.registrarActividad("Tarea", "Fecha inválida", "20/10/2026", "Alta", "", false));
        debeFallar(() -> gestor.registrarActividad("Examen", "Sin materia", "2026-10-20", "Alta", "", false));
        debeFallar(() -> gestor.registrarActividad("Otro", "Tipo inválido", "2026-10-20", "Alta", "", false));
        debeFallar(() -> gestor.registrarActividad("Tarea", "Prioridad inválida", "2026-10-20", "Urgente", "", false));
        LocalDateTime vencimiento = tarea.getSiguienteRecordatorio();
        comprobar(gestor.obtenerRecordatoriosPendientes(vencimiento.minusSeconds(1)).isEmpty(), "Avisó antes de tiempo.");
        comprobar(gestor.obtenerRecordatoriosPendientes(vencimiento).contains(tarea), "No encontró el aviso.");
        gestor.confirmarRecordatorio(tarea, vencimiento.plusHours(30));
        comprobar(tarea.getSiguienteRecordatorio().equals(vencimiento.plusHours(36)), "Perdió los intervalos de 12 horas.");
        gestor.cambiarRecordatorio(tarea);
        comprobar(!tarea.isRecordatorioActivo(), "No desactivó el aviso.");
        gestor.cambiarRecordatorio(tarea);
        gestor.marcarComoCompletada(tarea);
        comprobar(tarea.isCompletada() && !tarea.isRecordatorioActivo(), "No completó o desactivó la actividad.");
        comprobar(gestor.obtenerResumen(null)[2] == 33, "Falló el porcentaje total.");
        comprobar(gestor.obtenerResumen(java.time.LocalDate.of(2026, 10, 20))[2] == 100, "Falló el porcentaje diario.");
        comprobar(gestor.listar("Completadas", "Todos", "", "Registro", null).equals(java.util.List.of(tarea)), "Falló el filtro de completadas.");
        gestor.registrarActividad("Tarea", "Urgente", "2026-10-19", "Baja", "", false);
        comprobar(gestor.listar("Todas", "Todos", "", "Fecha", null).get(0).getNombre().equals("Urgente"), "No ordenó por fecha.");
        comprobar(gestor.listar("Todas", "Todos", "", "Prioridad", null).get(0) == tarea, "No ordenó por prioridad.");
        debeFallar(() -> gestor.cambiarRecordatorio(tarea));
        comprobar(gestor.obtenerRecordatoriosPendientes(vencimiento.plusDays(5)).isEmpty(), "Avisó por una actividad completada.");
        GestorAcademicoController bloqueado = new GestorAcademicoController();
        for (int i = 0; i < 3; i++) { bloqueado.iniciarSesion("william", "mala"); }
        comprobar(bloqueado.isAccesoBloqueado(), "No bloqueó los intentos.");
        comprobar(!bloqueado.iniciarSesion("william", "UvG2026!"), "Ignoró el bloqueo.");
        gestor.cerrarSesion();
        debeFallar(() -> gestor.getActividades());
        comprobar(gestor.iniciarSesion("william", "UvG2026!"), "No pudo reingresar.");
        comprobar(gestor.getActividades().size() == 4, "Perdió datos al cerrar sesión sin cerrar el programa.");
        System.out.println("Todas las pruebas de lógica pasaron.");
    }
}
