import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GestorAcademicoController controlador = new GestorAcademicoController();
                GestorAcademicoView vista = new GestorAcademicoView(controlador);
                vista.setVisible(true);
            }
        });
    }
}
