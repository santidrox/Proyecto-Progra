import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Usuario {
    private final String nombreUsuario;
    private final byte[] hashContrasena;

    public Usuario(String nombreUsuario, String contrasena) {
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()
                || contrasena == null || contrasena.isEmpty()) {
            throw new IllegalArgumentException("Usuario y contraseña son obligatorios.");
        }
        this.nombreUsuario = nombreUsuario.trim();
        hashContrasena = hash(contrasena);
    }

    public String getNombreUsuario() { return nombreUsuario; }
    public boolean verificarContrasena(String contrasena) {
        return contrasena != null && MessageDigest.isEqual(hashContrasena, hash(contrasena));
    }

    private static byte[] hash(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("No se pudo verificar la contraseña.", error);
        }
    }
}
