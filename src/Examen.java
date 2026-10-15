public class Examen extends Actividad {
    private final String materia;

    public Examen(String nombre, java.time.LocalDate fecha, String prioridad, String materia) {
        super(nombre, fecha, prioridad);
        if (materia == null || materia.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingresa la materia del examen.");
        }
        this.materia = materia.trim();
    }

    public String getMateria() { return materia; }
    @Override public String getTipo() { return "Examen"; }
    @Override public String getDetalle() { return materia; }
}
