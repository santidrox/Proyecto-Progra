import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import javax.swing.*;


public class RegistrarTareas extends JFrame {

    private final GestorAcademicoController controlador =
            new GestorAcademicoController();
    private final DefaultListModel<Tarea> modelo = new DefaultListModel<>();
    private final JList<Tarea> lista = new JList<>(modelo);

    private final JTextField nombre = new JTextField();
    private final JTextField fecha = new JTextField();
    private final JTextField descripcion = new JTextField();
    private final JComboBox<String> prioridad = new JComboBox<>(
            new String[]{"Alta", "Media", "Baja"}
    );

    public RegistrarTareas() {
        setTitle("Registrar tareas académicas");
        setSize(750, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel formulario = new JPanel(new GridLayout(4, 2, 8, 8));
        formulario.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        formulario.add(new JLabel("Nombre de la tarea:"));
        formulario.add(nombre);
        formulario.add(new JLabel("Fecha de entrega (AAAA-MM-DD):"));
        formulario.add(fecha);
        formulario.add(new JLabel("Prioridad:"));
        formulario.add(prioridad);
        formulario.add(new JLabel("Descripción (opcional):"));
        formulario.add(descripcion);

        JButton registrar = new JButton("Registrar tarea");
        JButton limpiar = new JButton("Limpiar campos");
        JPanel botones = new JPanel();
        botones.add(registrar);
        botones.add(limpiar);

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tareasRegistradas = new JScrollPane(lista);
        tareasRegistradas.setBorder(
                BorderFactory.createTitledBorder("Tareas registradas")
        );

        add(formulario, BorderLayout.NORTH);
        add(tareasRegistradas, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        registrar.addActionListener(evento -> registrarTarea());
        limpiar.addActionListener(evento -> limpiarCampos());
        getRootPane().setDefaultButton(registrar);
    }

    private void registrarTarea() {
        try {
            Tarea tarea = controlador.registrarTarea(
                    nombre.getText(),
                    fecha.getText(),
                    (String) prioridad.getSelectedItem(),
                    descripcion.getText()
            );

            modelo.addElement(tarea);
            lista.ensureIndexIsVisible(modelo.size() - 1);
            mostrarMensaje("Tarea registrada correctamente.");
            limpiarCampos();
        } catch (IllegalArgumentException error) {
            mostrarMensaje(error.getMessage());
        }
    }

    private void limpiarCampos() {
        nombre.setText("");
        fecha.setText("");
        descripcion.setText("");
        prioridad.setSelectedIndex(0);
        nombre.requestFocusInWindow();
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }

    private static class GestorAcademicoController {
        private final ArrayList<Actividad> actividades = new ArrayList<>();

        public Tarea registrarTarea(String nombre, String fecha,
                                   String prioridad, String descripcion) {
            if (nombre == null || nombre.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Ingresa el nombre de la tarea."
                );
            }

            if (fecha == null || !fecha.trim().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                throw new IllegalArgumentException(
                        "Ingresa una fecha válida: AAAA-MM-DD."
                );
            }

            LocalDate fechaEntrega;
            try {
                fechaEntrega = LocalDate.parse(fecha.trim());
            } catch (DateTimeParseException error) {
                throw new IllegalArgumentException(
                        "La fecha no existe. Usa el formato AAAA-MM-DD."
                );
            }

            if (!"Alta".equals(prioridad) && !"Media".equals(prioridad)
                    && !"Baja".equals(prioridad)) {
                throw new IllegalArgumentException(
                        "Selecciona una prioridad: Alta, Media o Baja."
                );
            }

            Tarea tarea = new Tarea(
                    nombre.trim(), fechaEntrega, prioridad,
                    descripcion == null ? "" : descripcion.trim()
            );
            agregarActividad(tarea);
            return tarea;
        }

        public void agregarActividad(Actividad actividad) {
            actividades.add(actividad);
        }
    }

  
    private static class Actividad {
        private final String nombre;
        private final LocalDate fechaEntrega;
        private final String prioridad;
        private final boolean completada;
        private final boolean recordatorioActivo;

        public Actividad(String nombre, LocalDate fechaEntrega, String prioridad) {
            this.nombre = nombre;
            this.fechaEntrega = fechaEntrega;
            this.prioridad = prioridad;
            this.completada = false;
            this.recordatorioActivo = false;
        }

        public String getNombre() {
            return nombre;
        }

        public LocalDate getFechaEntrega() {
            return fechaEntrega;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public boolean isCompletada() {
            return completada;
        }

        public boolean isRecordatorioActivo() {
            return recordatorioActivo;
        }

        @Override
        public String toString() {
            return nombre + " | Entrega: " + fechaEntrega
                    + " | Prioridad: " + prioridad
                    + " | " + (completada ? "Completada" : "Pendiente");
        }
    }

    private static class Tarea extends Actividad {
        private final String descripcion;

        public Tarea(String nombre, LocalDate fechaEntrega,
                     String prioridad, String descripcion) {
            super(nombre, fechaEntrega, prioridad);
            this.descripcion = descripcion;
        }

        public String getDescripcion() {
            return descripcion;
        }

        @Override
        public String toString() {
            return "Tarea: " + super.toString()
                    + (descripcion.isEmpty() ? "" : " | " + descripcion);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new RegistrarTareas().setVisible(true)
        );
    }
}
