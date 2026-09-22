import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class MostrarRecordatorio extends JFrame {

    private final ArrayList<Actividad> actividades = new ArrayList<>();
    private final DefaultListModel<Actividad> modelo = new DefaultListModel<>();
    private final JList<Actividad> lista = new JList<>(modelo);

    private final JTextField nombre = new JTextField();
    private final JTextField fecha = new JTextField();

    private final JComboBox<String> tipo = new JComboBox<>(
            new String[]{"Tarea", "Proyecto", "Examen"}
    );

    private final JComboBox<String> prioridad = new JComboBox<>(
            new String[]{"Alta", "Media", "Baja"}
    );

    private final JCheckBox activar = new JCheckBox(
            "Activar recordatorios cada 12 horas"
    );

    public MostrarRecordatorio() {
        setTitle("Recordatorios académicos");
        setSize(650, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel formulario = new JPanel(new GridLayout(5, 2, 8, 8));
        formulario.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        formulario.add(new JLabel("Nombre:"));
        formulario.add(nombre);
        formulario.add(new JLabel("Tipo de actividad:"));
        formulario.add(tipo);
        formulario.add(new JLabel("Fecha de entrega (AAAA-MM-DD):"));
        formulario.add(fecha);
        formulario.add(new JLabel("Prioridad:"));
        formulario.add(prioridad);
        formulario.add(activar);

        JButton registrar = new JButton("Registrar actividad");
        formulario.add(registrar);

        JButton cambiarRecordatorio = new JButton(
                "Activar / desactivar recordatorio"
        );
        JButton completar = new JButton("Marcar como completada");

        JPanel botones = new JPanel();
        botones.add(cambiarRecordatorio);
        botones.add(completar);

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(formulario, BorderLayout.NORTH);
        add(new JScrollPane(lista), BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        registrar.addActionListener(evento -> registrarActividad());
        cambiarRecordatorio.addActionListener(
                evento -> cambiarRecordatorio()
        );
        completar.addActionListener(evento -> completarActividad());
        
        Timer temporizador = new Timer(
                60_000, evento -> mostrarRecordatorios()
        );
        temporizador.start();
    }

    private void registrarActividad() {
        String nombreActividad = nombre.getText().trim();

        if (nombreActividad.isEmpty()) {
            mostrarMensaje("Ingresa el nombre de la actividad.");
            return;
        }

        try {
            LocalDate fechaEntrega = LocalDate.parse(
                    fecha.getText().trim()
            );

            Actividad actividad = new Actividad(
                    nombreActividad,
                    (String) tipo.getSelectedItem(),
                    fechaEntrega,
                    (String) prioridad.getSelectedItem(),
                    activar.isSelected()
            );

            actividades.add(actividad);
            modelo.addElement(actividad);

            nombre.setText("");
            fecha.setText("");
            activar.setSelected(false);

        } catch (DateTimeParseException error) {
            mostrarMensaje("Ingresa una fecha válida: AAAA-MM-DD.");
        }
    }

    private void cambiarRecordatorio() {
        Actividad actividad = lista.getSelectedValue();

        if (actividad == null) {
            mostrarMensaje("Selecciona una actividad.");
            return;
        }

        if (actividad.completada) {
            mostrarMensaje("Esta actividad ya está completada.");
            return;
        }

        actividad.recordatorioActivo = !actividad.recordatorioActivo;
        lista.repaint();
    }

    private void completarActividad() {
        Actividad actividad = lista.getSelectedValue();

        if (actividad == null) {
            mostrarMensaje("Selecciona una actividad.");
            return;
        }

        actividad.completada = true;
        actividad.recordatorioActivo = false;
        lista.repaint();
    }

    private void mostrarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now();
        StringBuilder mensaje = new StringBuilder();

        for (Actividad actividad : actividades) {
            if (actividad.recordatorioActivo
                    && !actividad.completada
                    && !ahora.isBefore(actividad.siguienteRecordatorio)) {

                mensaje.append(actividad.tipo)
                        .append(": ").append(actividad.nombre)
                        .append("\nEntrega: ").append(actividad.fechaEntrega)
                        .append("\nPrioridad: ").append(actividad.prioridad)
                        .append("\n\n");

                do {
                    actividad.siguienteRecordatorio =
                            actividad.siguienteRecordatorio.plusHours(12);
                } while (!actividad.siguienteRecordatorio.isAfter(ahora));
            }
        }

        if (mensaje.length() > 0) {
            JOptionPane.showMessageDialog(
                    this,
                    mensaje.toString(),
                    "Recordatorios pendientes",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje);
    }

    private static class Actividad {
        private final String nombre;
        private final String tipo;
        private final LocalDate fechaEntrega;
        private final String prioridad;
        private boolean recordatorioActivo;
        private boolean completada;
        private LocalDateTime siguienteRecordatorio;

        public Actividad(String nombre, String tipo,
                         LocalDate fechaEntrega, String prioridad,
                         boolean recordatorioActivo) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.fechaEntrega = fechaEntrega;
            this.prioridad = prioridad;
            this.recordatorioActivo = recordatorioActivo;
            this.completada = false;
            this.siguienteRecordatorio =
                    LocalDateTime.now().plusHours(12);
        }

        @Override
        public String toString() {
            String estado = completada ? "Completada" : "Pendiente";
            String aviso = recordatorioActivo ? "Activado" : "Desactivado";

            return tipo + ": " + nombre
                    + " | " + estado
                    + " | Recordatorio: " + aviso;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new MostrarRecordatorio().setVisible(true)
        );
    }
}