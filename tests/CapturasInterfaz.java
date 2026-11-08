import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.LocalDate;
import javax.imageio.ImageIO;
import javax.swing.*;

// Revisa los paneles reales de Swing sin abrir ventanas ni usar los datos de la agenda.
public class CapturasInterfaz {
    private static void distribuir(Container panel) {
        panel.doLayout();
        for (Component hijo : panel.getComponents()) {
            if (hijo instanceof Container) { distribuir((Container) hijo); }
        }
    }

    private static Component buscar(Container padre, String texto, Class<?> tipo) {
        for (Component hijo : padre.getComponents()) {
            if (tipo.isInstance(hijo)) {
                if (texto == null || (hijo instanceof JButton && texto.equals(((JButton)hijo).getText()))) return hijo;
            }
            if (hijo instanceof Container) {
                Component resultado = buscar((Container)hijo, texto, tipo);
                if (resultado != null) return resultado;
            }
        }
        return null;
    }

    private static void imagen(JRootPane raiz, Path archivo, int ancho, int alto) throws Exception {
        raiz.setSize(ancho,alto); distribuir(raiz); distribuir(raiz);
        BufferedImage imagen = new BufferedImage(ancho,alto,BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0,0,ancho,alto);
        raiz.printAll(g); g.dispose(); ImageIO.write(imagen,"png",archivo.toFile());
    }

    public static void main(String[] args) throws Exception {
        final Path salida = Paths.get(args.length == 0 ? "capturas" : args[0]); Files.createDirectories(salida);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override public void run() {
                EstiloUVG.aplicar();
                GestorAcademicoController gestor = new GestorAcademicoController();
                GestorAcademicoView vista = new GestorAcademicoView(gestor);
                try {
                    JRootPane panel = vista.getContenido();
                    imagen(panel,salida.resolve("login.png"),1200,740);
                    JTextField usuario = (JTextField)buscar(panel,null,JTextField.class);
                    JPasswordField clave = (JPasswordField)buscar(panel,null,JPasswordField.class);
                    usuario.setText("william"); clave.setText("UvG2026!");
                    ((JButton)buscar(panel,"Entrar",JButton.class)).doClick();
                    imagen(panel,salida.resolve("agenda-vacia.png"),1200,740);
                    LocalDate hoy = LocalDate.now();
                    gestor.registrarActividad("Tarea","Ejercicio de programación",hoy.toString(),"Alta","POO",true);
                    gestor.registrarActividad("Proyecto","Entrega del gestor académico",hoy.plusDays(2).toString(),"Alta","Proyecto final",true);
                    gestor.registrarActividad("Examen","Parcial de matemática",hoy.plusDays(4).toString(),"Media","Matemática",false);
                    Actividad lectura = gestor.registrarActividad("Tarea","Lectura para la clase",hoy.toString(),"Baja","Capítulo 4",false);
                    gestor.marcarComoCompletada(lectura);
                    JComboBox<?> filtro = (JComboBox<?>)buscar(panel,null,JComboBox.class);
                    filtro.setSelectedIndex(1); filtro.setSelectedIndex(0);
                    imagen(panel,salida.resolve("agenda.png"),1200,740);
                    imagen(panel,salida.resolve("agenda-compacta.png"),1040,690);
                    JTable tabla = (JTable)buscar(panel,null,JTable.class);
                    if (tabla.getRowCount()!=4) throw new AssertionError("No se refrescó la tabla.");
                    tabla.setRowSelectionInterval(0,0);
                    ((JButton)buscar(panel,"Completar / reabrir",JButton.class)).doClick();
                    if (!gestor.getActividades().get(0).isCompletada()) throw new AssertionError("No se completó la selección.");
                    filtro.setSelectedItem("Pendientes");
                    if(tabla.getRowCount()!=2) throw new AssertionError("Falló el filtro visual.");
                    System.out.println("Paneles renderizados y eventos de acceso, selección y filtros correctos.");
                } catch (Exception error) { throw new RuntimeException(error); }
                finally { vista.detenerTemporizador(); }
            }
        });
    }
}
