import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// Apariencia de Swing sin librerías adicionales.
public class EstiloUVG {
    public static final Color VERDE = new Color(0, 141, 54);
    public static final Color TEXTO = new Color(35, 45, 39);
    public static final Color FONDO = new Color(247, 248, 247);
    public static final Color BORDE = new Color(223, 229, 224);
    public static final Color SUAVE = new Color(235, 246, 238);

    public static void aplicar() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception error) { System.err.println("Se usará la apariencia disponible."); }
        Font fuente = new Font("SansSerif", Font.PLAIN, 13);
        for (Object clave : UIManager.getDefaults().keySet().toArray()) {
            if (clave.toString().endsWith(".font")) { UIManager.put(clave, fuente); }
        }
        UIManager.put("Panel.background", Color.WHITE);
        UIManager.put("Label.foreground", TEXTO);
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.selectionBackground", SUAVE);
        UIManager.put("ComboBox.selectionForeground", TEXTO);
        UIManager.put("TextField.selectionBackground", new Color(204, 233, 212));
        UIManager.put("PasswordField.selectionBackground", new Color(204, 233, 212));
        UIManager.put("ProgressBar.foreground", VERDE);
        UIManager.put("ProgressBar.selectionBackground", TEXTO);
        UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
    }

    public static JLabel logo(int lado) {
        JLabel etiqueta = new JLabel();
        etiqueta.setToolTipText("Universidad del Valle de Guatemala");
        try {
            ImageIO.setUseCache(false);
            URL recurso = EstiloUVG.class.getResource("/assets/logo-uvg.png");
            BufferedImage imagen;
            if (recurso != null) { imagen = ImageIO.read(recurso); }
            else {
                Path base = Paths.get(EstiloUVG.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (Files.isRegularFile(base) || base.getFileName().toString().equals("bin")) { base = base.getParent(); }
                imagen = ImageIO.read(base.resolve("assets/logo-uvg.png").toFile());
            }
            int ancho = lado;
            int alto = Math.max(1, imagen.getHeight() * ancho / imagen.getWidth());
            BufferedImage copia = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = copia.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(imagen, 0, 0, ancho, alto, null); g.dispose();
            etiqueta.setIcon(new ImageIcon(copia));
        } catch (Exception error) {
            System.err.println("No se pudo cargar el logo UVG: " + error.getMessage());
            etiqueta.setText("UVG"); etiqueta.setForeground(VERDE);
            etiqueta.setFont(new Font("SansSerif", Font.BOLD, 20));
        }
        return etiqueta;
    }

    public static class Boton extends JButton {
        private static final long serialVersionUID = 1L;
        private final boolean principal;
        public Boton(String texto, boolean principal) {
            super(texto); this.principal = principal;
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(principal ? Color.WHITE : TEXTO);
            setBorder(new EmptyBorder(10, 14, 10, 14));
            setFocusPainted(false); setContentAreaFilled(false); setOpaque(false);
            setRolloverEnabled(true); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics graficos) {
            Graphics2D g = (Graphics2D) graficos.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fondo = principal ? VERDE : Color.WHITE;
            if (getModel().isRollover() && isEnabled()) { fondo = principal ? VERDE.darker() : SUAVE; }
            if (!isEnabled()) { fondo = new Color(238, 240, 238); }
            g.setColor(fondo); g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 9, 9);
            g.setColor(principal ? fondo : BORDE); g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 9, 9);
            if (isFocusOwner()) {
                g.setColor(principal ? Color.WHITE : VERDE);
                g.drawRoundRect(3, 3, getWidth()-7, getHeight()-7, 7, 7);
            }
            g.dispose(); super.paintComponent(graficos);
        }
    }
}
