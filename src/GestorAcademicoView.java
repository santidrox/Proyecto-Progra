import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class GestorAcademicoView extends JFrame {
    private static final long serialVersionUID = 1L;
    private final GestorAcademicoController controlador;
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel contenido = new JPanel(tarjetas);
    private final JTextField usuario = new JTextField(18);
    private final JPasswordField contrasena = new JPasswordField(18);
    private final JButton ingresar = new JButton("Iniciar sesión");
    private final JTextField nombre = new JTextField(22);
    private final JTextField fecha = new JTextField(22);
    private final JTextField detalle = new JTextField(22);
    private final JLabel etiquetaDetalle = new JLabel("Descripción (opcional):");
    private final JComboBox<String> tipo = new JComboBox<>(new String[]{"Tarea", "Proyecto", "Examen"});
    private final JComboBox<String> prioridad = new JComboBox<>(new String[]{"Alta", "Media", "Baja"});
    private final JCheckBox activar = new JCheckBox("Recordatorio cada 12 horas");
    private final JLabel bienvenida = new JLabel();
    private final DefaultTableModel modelo = new DefaultTableModel(
            new String[]{"Tipo", "Nombre", "Fecha", "Prioridad", "Detalle", "Estado", "Recordatorio"}, 0) {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final Timer temporizador;
    private boolean avisoAbierto;

    public GestorAcademicoView(GestorAcademicoController controlador) {
        this.controlador = controlador;
        setTitle("Gestor académico");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1050, 650);
        setMinimumSize(new java.awt.Dimension(850, 550));
        contenido.add(crearLogin(), "login");
        contenido.add(crearPrincipal(), "principal");
        setContentPane(contenido);
        setLocationRelativeTo(null);
        getRootPane().setDefaultButton(ingresar);
        temporizador = new Timer(60_000, evento -> mostrarRecordatorios());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent evento) {
                int respuesta = JOptionPane.showConfirmDialog(GestorAcademicoView.this,
                        "Esta versión no guarda los datos al cerrar. ¿Deseas salir?",
                        "Salir", JOptionPane.YES_NO_OPTION);
                if (respuesta == JOptionPane.YES_OPTION) {
                    temporizador.stop();
                    dispose();
                }
            }
        });
    }

    private JPanel crearLogin() {
        JPanel exterior = new JPanel(new GridBagLayout());
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Inicia sesión"));
        agregarCampo(formulario, 0, new JLabel("Usuario:"), usuario);
        agregarCampo(formulario, 1, new JLabel("Contraseña:"), contrasena);
        agregarCampo(formulario, 2, new JLabel(""), ingresar);
        ingresar.addActionListener(evento -> iniciarSesion());
        exterior.add(formulario);
        return exterior;
    }

    private JPanel crearPrincipal() {
        JPanel principal = new JPanel(new BorderLayout(10, 10));
        principal.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JPanel superior = new JPanel(new BorderLayout(8, 8));
        superior.add(bienvenida, BorderLayout.NORTH);
        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Registrar actividad"));
        agregarCampo(formulario, 0, new JLabel("Tipo:"), tipo);
        agregarCampo(formulario, 1, new JLabel("Nombre:"), nombre);
        agregarCampo(formulario, 2, new JLabel("Fecha (AAAA-MM-DD):"), fecha);
        agregarCampo(formulario, 3, new JLabel("Prioridad:"), prioridad);
        agregarCampo(formulario, 4, etiquetaDetalle, detalle);
        JPanel accionesRegistro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton registrar = new JButton("Registrar actividad");
        accionesRegistro.add(activar);
        accionesRegistro.add(registrar);
        agregarCampo(formulario, 5, new JLabel(""), accionesRegistro);
        tipo.addActionListener(evento -> etiquetaDetalle.setText(
                "Examen".equals(tipo.getSelectedItem()) ? "Materia (obligatoria):" : "Descripción (opcional):"));
        registrar.addActionListener(evento -> registrarActividad());
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(new JLabel("Aviso: los datos se mantienen solo mientras el programa está abierto."), BorderLayout.SOUTH);
        principal.add(superior, BorderLayout.NORTH);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(26);
        principal.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton completar = new JButton("Marcar como completada");
        JButton recordatorio = new JButton("Activar / desactivar recordatorio");
        acciones.add(completar);
        acciones.add(recordatorio);
        completar.addActionListener(evento -> modificarSeleccionada(true));
        recordatorio.addActionListener(evento -> modificarSeleccionada(false));
        principal.add(acciones, BorderLayout.SOUTH);
        return principal;
    }

    private void agregarCampo(JPanel panel, int fila, JComponent etiqueta, JComponent campo) {
        GridBagConstraints posicion = new GridBagConstraints();
        posicion.gridy = fila;
        posicion.gridx = 0;
        posicion.anchor = GridBagConstraints.WEST;
        posicion.insets = new Insets(4, 6, 4, 6);
        panel.add(etiqueta, posicion);
        posicion.gridx = 1;
        posicion.weightx = 1;
        posicion.fill = GridBagConstraints.HORIZONTAL;
        panel.add(campo, posicion);
    }

    private void iniciarSesion() {
        char[] clave = contrasena.getPassword();
        boolean correcto;
        try {
            correcto = controlador.iniciarSesion(usuario.getText(), new String(clave));
        } finally {
            Arrays.fill(clave, '\0');
            contrasena.setText("");
        }
        if (!correcto) {
            if (controlador.isAccesoBloqueado()) {
                ingresar.setEnabled(false);
                mensaje("Se agotaron los 3 intentos. Cierra y vuelve a abrir la aplicación para reintentar.");
            } else {
                mensaje("Usuario o contraseña incorrectos. Intentos restantes: " + controlador.getIntentosRestantes());
            }
            return;
        }
        bienvenida.setText("Bienvenido, " + controlador.getUsuarioActual().getNombreUsuario());
        tarjetas.show(contenido, "principal");
        getRootPane().setDefaultButton(null);
        actualizarTabla();
        temporizador.start();
        nombre.requestFocusInWindow();
    }

    private void registrarActividad() {
        try {
            controlador.registrarActividad((String) tipo.getSelectedItem(), nombre.getText(),
                    fecha.getText(), (String) prioridad.getSelectedItem(), detalle.getText(), activar.isSelected());
            actualizarTabla();
            nombre.setText("");
            fecha.setText("");
            detalle.setText("");
            activar.setSelected(false);
            nombre.requestFocusInWindow();
        } catch (IllegalArgumentException | IllegalStateException error) {
            mensaje(error.getMessage());
        }
    }

    private void modificarSeleccionada(boolean completar) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { mensaje("Selecciona una actividad de la tabla."); return; }
        Actividad actividad = controlador.getActividades().get(tabla.convertRowIndexToModel(fila));
        try {
            if (completar) { controlador.marcarComoCompletada(actividad); }
            else { controlador.cambiarRecordatorio(actividad); }
            actualizarTabla();
            tabla.setRowSelectionInterval(fila, fila);
        } catch (IllegalArgumentException | IllegalStateException error) {
            mensaje(error.getMessage());
        }
    }

    private void actualizarTabla() {
        modelo.setRowCount(0);
        for (Actividad actividad : controlador.getActividades()) {
            modelo.addRow(new Object[]{actividad.getTipo(), actividad.getNombre(), actividad.getFechaEntrega(),
                    actividad.getPrioridad(), actividad.getDetalle(),
                    actividad.isCompletada() ? "Completada" : "Pendiente",
                    actividad.isRecordatorioActivo() ? "Activado" : "Desactivado"});
        }
    }

    private void mostrarRecordatorios() {
        if (avisoAbierto) { return; }
        LocalDateTime ahora = LocalDateTime.now();
        List<Actividad> pendientes = controlador.obtenerRecordatoriosPendientes(ahora);
        if (pendientes.isEmpty()) { return; }
        StringBuilder texto = new StringBuilder("Tienes actividades pendientes:\n\n");
        for (Actividad actividad : pendientes) {
            texto.append(actividad.getTipo()).append(": ").append(actividad.getNombre())
                    .append("\nEntrega: ").append(actividad.getFechaEntrega())
                    .append("\nPrioridad: ").append(actividad.getPrioridad()).append("\n\n");
            controlador.confirmarRecordatorio(actividad, ahora);
        }
        avisoAbierto = true;
        try {
            JTextArea aviso = new JTextArea(texto.toString(), 15, 45);
            aviso.setEditable(false);
            aviso.setCaretPosition(0);
            JOptionPane.showMessageDialog(this, new JScrollPane(aviso),
                    "Recordatorios académicos", JOptionPane.INFORMATION_MESSAGE);
        } finally {
            avisoAbierto = false;
        }
    }

    private void mensaje(String texto) { JOptionPane.showMessageDialog(this, texto); }
}
