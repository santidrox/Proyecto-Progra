import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GestorAcademicoController {
    private final ArrayList<Usuario> usuarios = new ArrayList<>();
    private final ArrayList<Actividad> actividades = new ArrayList<>();
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

    public void cerrarSesion() {
        usuarioActual = null;
        intentosFallidos = 0;
    }

    public int[] obtenerResumen(LocalDate dia) {
        exigirSesion();
        int total = 0, completadas = 0;
        for (Actividad actividad : actividades) {
            if (dia == null || actividad.getFechaEntrega().equals(dia)) {
                total++;
                if (actividad.isCompletada()) { completadas++; }
            }
        }
        return new int[]{completadas, total, total == 0 ? 0 : completadas * 100 / total};
    }

    public List<Actividad> listar(String estado, String tipo, String texto, String orden, LocalDate dia) {
        exigirSesion();
        String busqueda = texto.trim().toLowerCase(Locale.ROOT);
        ArrayList<Actividad> resultado = new ArrayList<>();

        // Cada condición corresponde a un filtro de la pantalla.
        for (Actividad actividad : actividades) {
            boolean coincideEstado = "Todas".equals(estado)
                    || ("Completadas".equals(estado) && actividad.isCompletada())
                    || ("Pendientes".equals(estado) && !actividad.isCompletada());
            boolean coincideTipo = "Todos".equals(tipo) || actividad.getTipo().equals(tipo);
            boolean coincideDia = dia == null || actividad.getFechaEntrega().equals(dia);
            String informacion = (actividad.getNombre() + " " + actividad.getDetalle()).toLowerCase(Locale.ROOT);
            boolean coincideBusqueda = informacion.contains(busqueda);

            if (coincideEstado && coincideTipo && coincideDia && coincideBusqueda) {
                resultado.add(actividad);
            }
        }

        // Ordenamiento por inserción: mueve cada elemento a su posición.
        // Se ordena una copia para conservar el orden de registro del sistema.
        if ("Fecha".equals(orden) || "Prioridad".equals(orden)) {
            for (int i = 1; i < resultado.size(); i++) {
                Actividad actual = resultado.get(i);
                int posicion = i - 1;
                while (posicion >= 0 && debeIrDespues(resultado.get(posicion), actual, orden)) {
                    resultado.set(posicion + 1, resultado.get(posicion));
                    posicion--;
                }
                resultado.set(posicion + 1, actual);
            }
        }
        return resultado;
    }

    private boolean debeIrDespues(Actividad primera, Actividad segunda, String orden) {
        if ("Fecha".equals(orden)) {
            return primera.getFechaEntrega().isAfter(segunda.getFechaEntrega());
        }
        return valorPrioridad(primera.getPrioridad()) > valorPrioridad(segunda.getPrioridad());
    }

    private int valorPrioridad(String prioridad) {
        if ("Alta".equals(prioridad)) { return 1; }
        if ("Media".equals(prioridad)) { return 2; }
        return 3;
    }

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
        return new ArrayList<>(actividades);
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
