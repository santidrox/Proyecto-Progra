# Gestor Académico

Proyecto de Programación Orientada a Objetos de la Universidad del Valle de Guatemala.

Integrantes: William Reyes, Santiago Escobar y Marlon Yaeggy.

## ¿De qué trata?

Es una aplicación de escritorio hecha en Java para organizar tareas, proyectos y exámenes. Permite registrar actividades, asignar prioridades, ordenar por fecha o prioridad, marcar lo terminado, consultar el progreso y activar recordatorios. La idea es ayudar a los estudiantes a organizar su carga académica.

## Abrir desde Java

1. Extrae el ZIP completo.
2. Abre en tu editor la carpeta `GestorAcademico`, que contiene `src`, `bin` y `lib`.
3. Configura un JDK 17 o superior para el proyecto.
4. Abre `src/Main.java` y ejecuta su método `main` con la opción Run/Ejecutar del editor.

No se necesitan archivos `.bat`, `.sh` ni Bash. El programa se inicia desde la clase `Main`.
En Visual Studio Code se necesita el soporte de Java instalado para que aparezca Run Java.

Cuenta de demostración:

- Usuario: `william`
- Contraseña: `UvG2026!`

La cuenta está definida en el constructor de `GestorAcademicoController`. Si la cambias, vuelve a compilar desde el editor. Después de tres intentos incorrectos hay que reiniciar el programa para reintentar.

## Carpetas

| Carpeta | Contenido |
|---|---|
| `src` | Código fuente `.java`, una clase principal por archivo. |
| `bin` | Código compilado `.class` con Java 17. |
| `lib` | Reservada para librerías externas. Actualmente no se usan. |
| `tests` | Pruebas de lógica. Se pueden ejecutar desde el `main` de `Pruebas.java`, incluyendo `src` y `tests` como carpetas de código. |

## Temas de Java utilizados

- Clases, objetos, constructores y encapsulamiento.
- Herencia: `Tarea`, `Proyecto` y `Examen` extienden `Actividad`.
- Abstracción: `Actividad` define los datos y comportamientos comunes.
- Polimorfismo: una lista de actividades contiene los tres tipos.
- Colecciones: `ArrayList`, ciclos `for` y condicionales para buscar y filtrar.
- Ordenamiento por inserción con ciclos y comparación de fechas o prioridades.
- Manejo de excepciones para datos incorrectos.
- Interfaces y clases anónimas, como `ActionListener`, para responder a eventos.
- Swing para las ventanas, botones, tabla y calendario.
- MVC: modelos para los datos, controlador para las operaciones y vista para la interfaz.

El código de `src` usa ciclos y listeners explícitos, sin streams, lambdas ni referencias a métodos.
Las pruebas auxiliares conservan algunas lambdas para comprobar operaciones que deben fallar.

## Archivos principales

| Archivo | Función |
|---|---|
| `Main.java` | Inicia el controlador y la vista en el hilo de eventos de Swing. |
| `Actividad.java` | Define nombre, fecha, prioridad, estado y programación de recordatorios. |
| `Tarea.java`, `Proyecto.java`, `Examen.java` | Representan los tipos de actividad mediante herencia. |
| `Usuario.java` | Representa la cuenta y verifica la contraseña. Conserva el hash SHA-256 del prototipo; la gestión de cuentas reales está pendiente. |
| `GestorAcademicoController.java` | Administra el acceso, registro, filtros, orden, progreso y avisos. |
| `GestorAcademicoView.java` | Muestra la interfaz y delega las operaciones al controlador. |

## Uso y limitaciones

Pulsa `Nueva actividad` para registrar una tarea, un proyecto o un examen. Para un examen se solicita la materia. Los controles superiores permiten buscar, filtrar y ordenar. El calendario filtra por fecha de entrega y `Ver todas las fechas` quita ese filtro. El doble clic en una fila muestra sus detalles.

El progreso total y el diario se calculan sobre toda la agenda, aunque haya filtros activos. El progreso diario cuenta las actividades cuya fecha de entrega es hoy. Si no hay actividades, se muestra 0 %.

Los recordatorios comienzan 12 horas después del registro y se revisan cada minuto mientras el programa está abierto. Al completar una actividad se desactiva su recordatorio. No son avisos enviados por correo ni notificaciones con el programa cerrado.

Los datos siguen en memoria y se pierden al cerrar el programa. Cerrar sesión conserva los datos de la cuenta de demostración hasta cerrar la aplicación. El guardado permanente y las cuentas persistentes están pendientes.

## Verificación

El código compila con Java 17 y pasaron las pruebas de lógica de acceso, validaciones, registro, filtros, ordenamiento, progreso y recordatorios. La apariencia y la interacción de las ventanas deben revisarse en una computadora con escritorio gráfico.
