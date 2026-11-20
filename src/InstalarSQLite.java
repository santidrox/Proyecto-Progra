import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

/** Utilidad opcional que descarga las dependencias oficiales en la computadora del usuario. */
public class InstalarSQLite {
    public static void main(String[] args) {
        try {
            Path salida = Path.of(InstalarSQLite.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path proyecto = Files.isRegularFile(salida) || salida.getFileName().toString().equals("bin")
                    ? salida.getParent() : salida;
            Path lib = proyecto.resolve("lib");
            Files.createDirectories(lib);
            descargar(lib, "org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar", "org/sqlite/JDBC.class");
            descargar(lib, "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar", "org/slf4j/Logger.class");
            descargar(lib, "org/slf4j/slf4j-nop/1.7.36/slf4j-nop-1.7.36.jar", "org/slf4j/impl/StaticLoggerBinder.class");
            System.out.println("Dependencias listas en " + lib);
            System.out.println("Vuelve a abrir el proyecto en VS Code y ejecuta PruebasSQLite. Después ejecuta Main.");
        } catch (Exception error) {
            System.err.println("No se pudieron preparar las dependencias: " + error.getMessage());
            System.err.println("Revisa tu conexión o descarga los JAR indicados en lib/LEEME.txt.");
            System.exit(1);
        }
    }

    private static void descargar(Path lib, String ruta, String clase) throws Exception {
        String nombre = ruta.substring(ruta.lastIndexOf('/') + 1);
        Path destino = lib.resolve(nombre);
        if (Files.isRegularFile(destino)) {
            verificar(destino, clase);
            System.out.println("Ya existe: " + nombre);
            return;
        }
        Path temporal = Files.createTempFile(lib, "descarga-", ".tmp");
        HttpURLConnection conexion = (HttpURLConnection) URI.create(
                "https://repo.maven.apache.org/maven2/" + ruta).toURL().openConnection();
        conexion.setConnectTimeout(15000);
        conexion.setReadTimeout(30000);
        try {
            if (conexion.getResponseCode() != 200) {
                throw new IllegalStateException("El servidor respondió " + conexion.getResponseCode() + " para " + nombre);
            }
            try (InputStream entrada = conexion.getInputStream()) {
                Files.copy(entrada, temporal, StandardCopyOption.REPLACE_EXISTING);
            }
            verificar(temporal, clase);
            Files.move(temporal, destino);
            System.out.println("Descargado: " + nombre);
        } finally {
            conexion.disconnect();
            Files.deleteIfExists(temporal);
        }
    }

    private static void verificar(Path archivo, String clase) throws Exception {
        try (JarFile jar = new JarFile(archivo.toFile())) {
            if (jar.getJarEntry(clase) == null) {
                throw new IllegalStateException("El JAR no contiene la clase esperada: " + archivo.getFileName());
            }
        }
    }
}
