# Gestor Académico

Proyecto de POO de William Reyes, Santiago Escobar y Marlon Yaeggy.

Esta aplicación de Java ayuda a organizar tareas, proyectos y exámenes. Incluye
prioridades, filtros, calendario, ordenamiento, progreso diario y recordatorios.

La interfaz usa blanco, gris claro y el verde tomado del logo UVG adjunto
(RGB 0, 141, 54). El logo original se muestra pequeño en la esquina superior derecha
del acceso y de la agenda. Se redujeron textos y se ajustaron botones, tabla y calendario.
Los mensajes y las confirmaciones conservan el comportamiento de la versión anterior.

## Abrir desde el editor

1. Extrae todo el ZIP.
2. Abre la carpeta `GestorAcademico` que contiene `src`, `bin`, `lib` y `data`.
3. Usa un JDK 17 o superior. En VS Code necesitas el soporte de Java instalado.
4. Abre `src/Main.java` y pulsa Run Java o Ejecutar.

No se incluyen archivos `.bat` ni `.sh`. No hay dependencias externas.
El ZIP contiene los `.class` y la configuración de carpetas para VS Code.

Cuenta de demostración: usuario `william`, contraseña `UvG2026!`.
Se configura en el constructor de `GestorAcademicoController` y sigue siendo
una cuenta fija. No es un sistema de registro de usuarios.

## Mejoras de esta versión

- Guardado automático después de registrar, editar, completar, reabrir o eliminar.
- Recuperación de las actividades al iniciar de nuevo.
- Conservación de la próxima fecha de recordatorio al cerrar o editar.
- Edición de nombre, fecha, prioridad y detalle sin cambiar el tipo original.
- Reapertura de actividades completadas; los avisos quedan desactivados hasta reactivarlos.
- Eliminación con confirmación y copia de los datos anteriores en `.bak`.
- Mensaje ante problemas de lectura o guardado, con reversión del cambio fallido.

## Datos y recordatorios

Se guarda en `data/actividades.xml`, junto al proyecto cuando se compila en `bin`.
Si el editor compila en otra ubicación, la carpeta `data` se crea junto a esa salida.
El archivo aparece al realizar el primer cambio. No borres `data` si quieres
conservar la agenda. La copia anterior es `actividades.xml.bak`; no está cifrada.
Utiliza una sola instancia de la aplicación a la vez para evitar sobrescribir
los cambios de otra ventana.

Los avisos siguen los intervalos de 12 horas desde el registro. Si cierras el programa,
no se muestran notificaciones en segundo plano. Al volver a iniciar sesión se
revisan los avisos atrasados y se conserva el horario original. Se revisa cada minuto
mientras la aplicación está abierta. Completar una actividad desactiva sus avisos.

Este guardado usa XML con las clases estándar de Java; NO es SQLite. La descarga
del controlador JDBC estuvo bloqueada en el entorno de preparación. Para hacer
la integración SQLite hace falta aportar el JAR y luego sustituir el repositorio
por uno JDBC. No se incluyen JAR ficticios ni se requiere instalar una base de datos
para ejecutar esta versión.

## Organización y temas de clase

- `src`: clases Java de modelo, controlador, vista y persistencia.
- `bin`: `.class` compilados.
- `lib`: reservada para futuras librerías.
- `assets`: logo original `logo-uvg.png`. También está en `bin/assets` para cargarlo como recurso.
- `data`: información local creada durante el uso.
- `tests`: pruebas de lógica y persistencia con datos temporales separados.

Se utilizan clases, encapsulamiento, herencia, polimorfismo, abstracción, MVC,
ArrayList, ciclos, ordenamiento por inserción, excepciones, archivos y eventos Swing.
`RepositorioActividades` se encarga de los archivos; la vista delega las operaciones
al controlador. El código de la aplicación no usa streams ni expresiones lambda.

## Comprobaciones

Se compila con Java 17. Ejecuta el `main` de `Pruebas.java` y el de
`PruebasPersistencia.java` para verificar la lógica y la lectura/escritura.
Estas pruebas usan carpetas temporales diferentes de la agenda real.
Se renderizaron y revisaron los paneles reales de acceso y agenda con Java en modo
sin pantalla, a tamaño normal y compacto. También se verificaron eventos de acceso,
selección, finalización y filtros. Los cuadros de diálogo y el comportamiento de la
ventana del sistema deben comprobarse en una computadora con escritorio.

La vista utiliza composición: contiene un JFrame para la ventana y un JRootPane
para su contenido. Esto permite revisar los mismos paneles sin abrir una ventana.
`EstiloUVG` reúne colores, tipografía, botones y carga del logo, sin librerías externas.

Antes de reemplazar una versión anterior, guarda una copia y prueba esta versión
en una carpeta aparte. Los cambios no se han subido automáticamente a GitHub.
