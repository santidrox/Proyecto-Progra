public class Tarea extends Actividad {
    private final String descripcion;

    public Tarea(String nombre, java.time.LocalDate fecha, String prioridad, String descripcion) {
        super(nombre, fecha, prioridad);
        this.descripcion = descripcion == null ? "" : descripcion.trim();
    }

    public String getDescripcion() { return descripcion; }
    @Override public String getTipo() { return "Tarea"; }
    @Override public String getDetalle() { return descripcion; }
}
