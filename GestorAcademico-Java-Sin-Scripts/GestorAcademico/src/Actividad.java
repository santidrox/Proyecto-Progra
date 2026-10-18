import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public abstract class Actividad {
    private final String nombre;
    private final LocalDate fechaEntrega;
    private final String prioridad;
    private boolean completada;
    private boolean recordatorioActivo;
    private LocalDateTime siguienteRecordatorio;

    protected Actividad(String nombre, LocalDate fechaEntrega, String prioridad) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingresa el nombre de la actividad.");
        }
        if (!"Alta".equals(prioridad) && !"Media".equals(prioridad)
                && !"Baja".equals(prioridad)) {
            throw new IllegalArgumentException("Selecciona una prioridad válida.");
        }
        this.nombre = nombre.trim();
        if (fechaEntrega == null) {
            throw new IllegalArgumentException("Ingresa una fecha.");
        }
        this.fechaEntrega = fechaEntrega;
        this.prioridad = prioridad;
        siguienteRecordatorio = LocalDateTime.now().plusHours(12);
    }

    public String getNombre() { return nombre; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public String getPrioridad() { return prioridad; }
    public boolean isCompletada() { return completada; }
    public boolean isRecordatorioActivo() { return recordatorioActivo; }
    public LocalDateTime getSiguienteRecordatorio() { return siguienteRecordatorio; }
    public abstract String getTipo();
    public abstract String getDetalle();

    public void marcarComoCompletada() {
        completada = true;
        recordatorioActivo = false;
    }

    public void setRecordatorioActivo(boolean activo) {
        if (activo && completada) {
            throw new IllegalStateException("La actividad ya está completada.");
        }
        recordatorioActivo = activo;
    }

    public boolean necesitaRecordatorio(LocalDateTime ahora) {
        return recordatorioActivo && !completada && !ahora.isBefore(siguienteRecordatorio);
    }

    public void avanzarRecordatorio(LocalDateTime ahora) {
        if (!necesitaRecordatorio(ahora)) { return; }
        long intervalos = Duration.between(siguienteRecordatorio, ahora).toHours() / 12 + 1;
        siguienteRecordatorio = siguienteRecordatorio.plusHours(intervalos * 12);
    }
}
