import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Requisito funcional Inicio de sesión.
 * Usuario: william
 * Contraseña: UvG2026!
 */
public class InicioSesion {

    private static final int MAXIMO_INTENTOS = 3;
    private final Map<String, String> usuarios = new HashMap<>();

    public InicioSesion() {
        // Se guarda el hash de la contraseña, no la contraseña en texto plano.
        registrarUsuario("william", "UvG2026!");
    }

    /**
     * Registra un usuario en el sistema.
     */
    public void registrarUsuario(String nombreUsuario, String contrasena) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }

        usuarios.put(nombreUsuario.trim().toLowerCase(), generarHash(contrasena));
    }

    /**
     * Comprueba si el usuario y la contraseña son correctos.
     */
    public boolean autenticar(String nombreUsuario, String contrasena) {
        if (nombreUsuario == null || nombreUsuario.isBlank()
                || contrasena == null || contrasena.isBlank()) {
            return false;
        }

        String hashGuardado = usuarios.get(nombreUsuario.trim().toLowerCase());

        if (hashGuardado == null) {
            return false;
        }

        String hashIngresado = generarHash(contrasena);
        return MessageDigest.isEqual(
                hashGuardado.getBytes(StandardCharsets.UTF_8),
                hashIngresado.getBytes(StandardCharsets.UTF_8)
        );
    }

    
    private String generarHash(String contrasena) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contrasena.getBytes(StandardCharsets.UTF_8));

            StringBuilder resultado = new StringBuilder();
            for (byte valor : hash) {
                resultado.append(String.format("%02x", valor));
            }
            return resultado.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo proteger la contraseña.", exception);
        }
    }

    public static void main(String[] args) {
        InicioSesion sistema = new InicioSesion();
        Scanner scanner = new Scanner(System.in);

        System.out.println("===============================");
        System.out.println("      GESTOR ACADÉMICO");
        System.out.println("===============================");

        for (int intento = 1; intento <= MAXIMO_INTENTOS; intento++) {
            System.out.print("Usuario: ");
            String usuario = scanner.nextLine();

            System.out.print("Contraseña: ");
            String contrasena = scanner.nextLine();

            if (sistema.autenticar(usuario, contrasena)) {
                System.out.println("Inicio de sesión correcto.");
                System.out.println("Bienvenido, " + usuario + ".");
                scanner.close();
                return;
            }

            int intentosRestantes = MAXIMO_INTENTOS - intento;
            System.out.println("Usuario o contraseña incorrectos.");

            if (intentosRestantes > 0) {
                System.out.println("Intentos restantes: " + intentosRestantes);
            }
        }

        System.out.println("Acceso bloqueado por demasiados intentos fallidos.");
        scanner.close();
    }
}
