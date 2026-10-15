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
        debeFallar(() -> gestor.cambiarRecordatorio(tarea));
        comprobar(gestor.obtenerRecordatoriosPendientes(vencimiento.plusDays(5)).isEmpty(), "Avisó por una actividad completada.");
        GestorAcademicoController bloqueado = new GestorAcademicoController();
        for (int i = 0; i < 3; i++) { bloqueado.iniciarSesion("william", "mala"); }
        comprobar(bloqueado.isAccesoBloqueado(), "No bloqueó los intentos.");
        comprobar(!bloqueado.iniciarSesion("william", "UvG2026!"), "Ignoró el bloqueo.");
        System.out.println("Todas las pruebas de lógica pasaron.");
    }
}
