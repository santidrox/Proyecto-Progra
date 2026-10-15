public class Proyecto extends Actividad {
    private final String descripcion;

    public Proyecto(String nombre, java.time.LocalDate fecha, String prioridad, String descripcion) {
        super(nombre, fecha, prioridad);
        this.descripcion = descripcion == null ? "" : descripcion.trim();
    }

    public String getDescripcion() { return descripcion; }
    @Override public String getTipo() { return "Proyecto"; }
    @Override public String getDetalle() { return descripcion; }
}
