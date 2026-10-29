import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

//Guarda directamente en la computadora local, falta sqlite
public class RepositorioActividades {
    private final Path archivo;

    public RepositorioActividades(Path archivo) {
        this.archivo = archivo.toAbsolutePath().normalize();
    }

    public ArrayList<Actividad> cargar() {
        ArrayList<Actividad> actividades = new ArrayList<>();
        if (!Files.exists(archivo)) { return actividades; }
        Properties datos = new Properties();
        try (InputStream entrada = Files.newInputStream(archivo)) {
            datos.loadFromXML(entrada);
            if (!"1".equals(datos.getProperty("version"))) {
                throw new IllegalArgumentException("Versión de datos desconocida.");
            }
            int cantidad = Integer.parseInt(requerido(datos, "cantidad"));
            if (cantidad < 0 || cantidad > 100000) {
                throw new IllegalArgumentException("Cantidad de actividades inválida.");
            }
            for (int i = 0; i < cantidad; i++) {
                String clave = "actividad." + i + ".";
                String tipo = requerido(datos, clave + "tipo");
                String nombre = requerido(datos, clave + "nombre");
                LocalDate fecha = LocalDate.parse(requerido(datos, clave + "fecha"));
                String prioridad = requerido(datos, clave + "prioridad");
                String detalle = requerido(datos, clave + "detalle");
                Actividad actividad;
                if ("Tarea".equals(tipo)) { actividad = new Tarea(nombre, fecha, prioridad, detalle); }
                else if ("Proyecto".equals(tipo)) { actividad = new Proyecto(nombre, fecha, prioridad, detalle); }
                else if ("Examen".equals(tipo)) { actividad = new Examen(nombre, fecha, prioridad, detalle); }
                else { throw new IllegalArgumentException("Tipo de actividad desconocido."); }
                actividad.restaurarEstado(booleano(datos, clave + "completada"),
                        booleano(datos, clave + "aviso"),
                        LocalDateTime.parse(requerido(datos, clave + "siguiente")));
                actividades.add(actividad);
            }
            return actividades;
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("No se pudieron leer los datos de " + archivo
                    + ". El archivo no fue reemplazado. Revisa la copia .bak o contacta al equipo.", error);
        }
    }

    private String requerido(Properties datos, String clave) {
        String valor = datos.getProperty(clave);
        if (valor == null) { throw new IllegalArgumentException("Falta un dato: " + clave); }
        return valor;
    }

    private boolean booleano(Properties datos, String clave) {
        String valor = requerido(datos, clave);
        if (!"true".equals(valor) && !"false".equals(valor)) {
            throw new IllegalArgumentException("Estado inválido: " + clave);
        }
        return Boolean.parseBoolean(valor);
    }

    public void guardar(List<Actividad> actividades) {
        Properties datos = new Properties();
        datos.setProperty("version", "1");
        datos.setProperty("cantidad", String.valueOf(actividades.size()));
        for (int i = 0; i < actividades.size(); i++) {
            Actividad a = actividades.get(i);
            String clave = "actividad." + i + ".";
            datos.setProperty(clave + "tipo", a.getTipo());
            datos.setProperty(clave + "nombre", a.getNombre());
            datos.setProperty(clave + "fecha", a.getFechaEntrega().toString());
            datos.setProperty(clave + "prioridad", a.getPrioridad());
            datos.setProperty(clave + "detalle", a.getDetalle());
            datos.setProperty(clave + "completada", String.valueOf(a.isCompletada()));
            datos.setProperty(clave + "aviso", String.valueOf(a.isRecordatorioActivo()));
            datos.setProperty(clave + "siguiente", a.getSiguienteRecordatorio().toString());
        }
        Path temporal = null;
        try {
            Files.createDirectories(archivo.getParent());
            temporal = Files.createTempFile(archivo.getParent(), "agenda-", ".tmp");
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                datos.storeToXML(salida, "Gestor Academico - actividades", "UTF-8");
            }
            if (Files.exists(archivo)) {
                Files.copy(archivo, archivo.resolveSibling(archivo.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo guardar en " + archivo
                    + ". El cambio no se aplicó. Revisa los permisos y el espacio disponible.", error);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); } catch (IOException error) { /* Se conserva el archivo de datos. */ }
            }
        }
    }
}
