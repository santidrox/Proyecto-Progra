# Cómo adaptamos el Gestor Académico a SQLite

Antes, `RepositorioActividades` escribía un archivo XML. Ahora su código utiliza
JDBC, la API de Java para conectarse a bases de datos. Se conservó su interfaz
de dos operaciones: `cargar()` y `guardar(List<Actividad>)`. Así, el controlador
y las pantallas siguen trabajando de la misma forma.

## 1. Archivo de base de datos

En `Main.java`, la ruta ahora termina en `data/actividades.db`. SQLite conserva
las tablas y los datos dentro de ese archivo. La carpeta se localiza desde la
salida de compilación, normalmente `bin`, y no desde Downloads ni una ruta fija
de Windows. El ZIP incluye una base vacía y el SQL de sus tablas.

## 2. Conexión

El método `conectar()` de `RepositorioActividades.java` carga `org.sqlite.JDBC`
y llama a `DriverManager.getConnection("jdbc:sqlite:" + archivo)`.
El prefijo `jdbc:sqlite:` le indica al controlador qué motor debe utilizar.
La ruta que sigue identifica el archivo local.

El controlador real y sus dependencias deben estar en `lib`. No se incluyeron
porque su descarga estuvo bloqueada. Sin ellos, la aplicación informa que falta
el controlador. Esta versión no ha pasado todavía la prueba completa JDBC.
Para descargarlos desde tu computadora, abre `src/InstalarSQLite.java` y usa
Run Java. La utilidad descarga las versiones oficiales en `lib`. Después vuelve
a abrir la carpeta del proyecto y ejecuta `PruebasSQLite` y `Main`.

## 3. Tablas

`preparar()` crea `actividades` si no existe. Sus columnas son:

| Columna | Para qué sirve |
| --- | --- |
| id | Orden dentro de la lista guardada |
| tipo | Tarea, Proyecto o Examen |
| nombre | Nombre de la actividad |
| fecha | Fecha de entrega en formato AAAA-MM-DD |
| prioridad | Alta, Media o Baja |
| detalle | Descripción o materia del examen |
| completada | 0 para pendiente, 1 para completada |
| recordatorio_activo | 0 para desactivado, 1 para activado |
| siguiente_recordatorio | Fecha y hora ISO del próximo aviso |

La tabla `agenda_control` guarda una revisión y el indicador de migración XML.
La cuenta de acceso sigue definida en el controlador; no se guardan usuarios
ni contraseñas en esta base.

## 4. Registro y guardado

Cuando se pulsa Guardar, la vista llama a `registrarActividad()` del controlador.
El controlador valida los datos, construye el objeto y lo agrega a su lista.
Después llama a `guardar()` del repositorio.

El repositorio desactiva el guardado automático de JDBC, comprueba la revisión
y vuelve a escribir la lista mediante `DELETE` e `INSERT` dentro de la misma
transacción. Este diseño sencillo es para una agenda personal pequeña. No
borra la base ni crea de nuevo sus tablas. Los identificadores de fila pueden
cambiar si se elimina una actividad.

El `INSERT` utiliza `PreparedStatement`: los signos `?` reciben valores con
`setString()` o `setInt()`. El nombre que escriba el usuario se trata como un
valor y no se concatena como una instrucción SQL.

Si todo sale bien, `commit()` confirma los cambios. Si ocurre un error,
`rollback()` revierte la transacción. El controlador también revierte su cambio
en memoria. Así no se muestra como guardada una actividad que no pudo persistirse.

## 5. Recuperación

`cargar()` ejecuta `SELECT * FROM actividades ORDER BY id`. Un `ResultSet`
permite recorrer sus filas. Según la columna tipo se crea un objeto `Tarea`,
`Proyecto` o `Examen`. Luego se restauran el estado y el horario del aviso.
El controlador recibe esos objetos y la vista muestra la lista.

## 6. Migración del XML anterior

Antes del primer inicio, coloca el XML anterior en `data/actividades.xml`.
Si no se ha migrado y la tabla está vacía, el programa importa los datos con
`RepositorioActividadesXML`. La importación y su indicador se confirman juntos.
El XML original no se modifica. Al borrar todas las actividades no reaparecerán
en el siguiente inicio, porque el indicador evita importarlas otra vez.

## 7. Cómo verificar la conexión

Después de agregar los JAR a `lib`, ejecuta `PruebasSQLite.java`. También registra
una tarea desde Main, cierra la aplicación, vuelve a abrirla y comprueba que
conserva su nombre, fecha, prioridad y estado. La prueba automatizada comprueba
además los avisos, las comillas, los errores de SQL, las ventanas desactualizadas
y la importación XML.

En un visor de SQLite puedes abrir `data/actividades.db` y ejecutar:

```sql
SELECT id, tipo, nombre, fecha, prioridad, completada FROM actividades;
```

La compilación Java, las pruebas de lógica, los paneles Swing y el esquema SQL
se comprobaron durante la preparación. Eso no sustituye la ejecución de
`PruebasSQLite` con el controlador real.
