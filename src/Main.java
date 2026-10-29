import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    java.nio.file.Path carpeta = java.nio.file.Paths.get(
                            Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    if (java.nio.file.Files.isRegularFile(carpeta) || carpeta.getFileName().toString().equals("bin")) {
                        carpeta = carpeta.getParent();
                    }
                    GestorAcademicoController controlador = new GestorAcademicoController(
                            carpeta.resolve("data").resolve("actividades.xml"));
                    GestorAcademicoView vista = new GestorAcademicoView(controlador);
                    vista.setVisible(true);
                } catch (Exception error) {
                    javax.swing.JOptionPane.showMessageDialog(null, error.getMessage(),
                            "No se pudo abrir la agenda", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
