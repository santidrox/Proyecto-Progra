import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GestorAcademicoController {
    private final List<Usuario> usuarios = new ArrayList<>();
    private final List<Actividad> actividades = new ArrayList<>();
    private Usuario usuarioActual;
    private int intentosFallidos;

    public GestorAcademicoController() {
        usuarios.add(new Usuario("william", "UvG2026!"));
    }

    public boolean iniciarSesion(String nombre, String contrasena) {
        if (isAccesoBloqueado()) { return false; }
        for (Usuario usuario : usuarios) {
            if (nombre != null && usuario.getNombreUsuario().equalsIgnoreCase(nombre.trim())
                    && usuario.verificarContrasena(contrasena)) {
                usuarioActual = usuario;
                intentosFallidos = 0;
                return true;
            }
        }
        intentosFallidos++;
        return false;
    }

    public boolean isAccesoBloqueado() { return intentosFallidos >= 3; }
    public int getIntentosRestantes() { return Math.max(0, 3 - intentosFallidos); }
    public Usuario getUsuarioActual() { return usuarioActual; }

    private void exigirSesion() {
        if (usuarioActual == null) { throw new IllegalStateException("Primero inicia sesión."); }
    }

    public Actividad registrarActividad(String tipo, String nombre, String fecha,
                                       String prioridad, String detalle, boolean recordatorio) {
        exigirSesion();
        LocalDate entrega;
        try {
            if (fecha == null || !fecha.trim().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                throw new IllegalArgumentException("Ingresa una fecha válida: AAAA-MM-DD.");
            }
            entrega = LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("La fecha no existe. Usa AAAA-MM-DD.");
        }
        Actividad actividad;
        if ("Tarea".equals(tipo)) {
            actividad = new Tarea(nombre, entrega, prioridad, detalle);
        } else if ("Proyecto".equals(tipo)) {
            actividad = new Proyecto(nombre, entrega, prioridad, detalle);
        } else if ("Examen".equals(tipo)) {
            actividad = new Examen(nombre, entrega, prioridad, detalle);
        } else {
            throw new IllegalArgumentException("Selecciona un tipo de actividad válido.");
        }
        actividad.setRecordatorioActivo(recordatorio);
        actividades.add(actividad);
        return actividad;
    }

    public List<Actividad> getActividades() {
        exigirSesion();
        return Collections.unmodifiableList(new ArrayList<>(actividades));
    }

    private void verificarActividad(Actividad actividad) {
        exigirSesion();
        if (!actividades.contains(actividad)) {
            throw new IllegalArgumentException("Selecciona una actividad registrada.");
        }
    }

    public void marcarComoCompletada(Actividad actividad) {
        verificarActividad(actividad);
        actividad.marcarComoCompletada();
    }

    public void cambiarRecordatorio(Actividad actividad) {
        verificarActividad(actividad);
        actividad.setRecordatorioActivo(!actividad.isRecordatorioActivo());
    }

    public List<Actividad> obtenerRecordatoriosPendientes(LocalDateTime ahora) {
        exigirSesion();
        List<Actividad> pendientes = new ArrayList<>();
        for (Actividad actividad : actividades) {
            if (actividad.necesitaRecordatorio(ahora)) { pendientes.add(actividad); }
        }
        return pendientes;
    }

    public void confirmarRecordatorio(Actividad actividad, LocalDateTime ahora) {
        verificarActividad(actividad);
        actividad.avanzarRecordatorio(ahora);
    }
}
