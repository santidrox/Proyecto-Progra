# Gestor académico — primera integración

Proyecto de William Reyes, Santiago Escobar y Marlon Yaeggy.

Esta versión conecta el inicio de sesión, registro de tareas/proyectos/exámenes,
prioridades, actividades completadas y recordatorios. Todos usan el mismo
controlador y la misma lista. No requiere librerías externas.

## Requisitos

JDK 17 o superior (no solamente JRE), con `java` y `javac` en PATH.
Se necesita un escritorio gráfico para la interfaz Swing.

## Estructura

- `src/`: modelos, controlador, vista y punto de entrada `Main.java`.
- `tests/`: pruebas de lógica sin dependencias externas.
- `bin/`: archivos `.class` compilados con Java 17, incluidos en el ZIP.
- `lib/`: carpeta reservada para librerías externas; vacía en esta versión.

No hay dependencias externas. No se incluye `data/` porque el guardado
todavía no está implementado.

## Compilar y ejecutar

Abre una terminal dentro de esta carpeta, donde están `src` y este README:

Como `bin/` ya contiene los compilados, puedes ejecutar directamente:

```sh
java -cp bin Main
```

Si modificas el código, vuelve a compilar antes de ejecutar:

```sh
javac -encoding UTF-8 -d bin src/*.java
java -cp bin Main
```

Cuenta de demostración conservada del código original:

- Usuario: `william`
- Contraseña: `UvG2026!`

Después de tres intentos fallidos hay que reiniciar la aplicación para reintentar.
Esta cuenta está definida en el código: no es un registro de usuarios completo.
El hash SHA-256 se conserva del prototipo; no es un esquema de contraseñas para
producción y deberá sustituirse al implementar cuentas persistentes.

## Probar la lógica

```sh
javac -encoding UTF-8 -d bin src/*.java tests/Pruebas.java
java -cp bin Pruebas
```

Las pruebas simulan las fechas pasando un instante al controlador; no cambian el
reloj ni requieren esperar 12 horas. Incluyen credenciales, bloqueo, validaciones,
tipos de actividad, lista compartida, avisos atrasados y finalización.

## Prueba manual de integración

1. Inicia sesión con la cuenta de demostración.
2. Registra una tarea con nombre, fecha `2026-10-20`, prioridad y recordatorio.
3. Comprueba que aparece en la tabla con el recordatorio activado.
4. Selecciónala y desactiva/reactiva el recordatorio.
5. Márcala como completada: debe cambiar su estado y desactivarse su aviso.
6. Intenta activar su aviso: debe explicar que ya está completada.
7. Registra un proyecto y un examen (con materia): deben aparecer en la misma tabla.
8. Prueba un nombre vacío y una fecha inexistente: no deben agregarse actividades.

## Alcance y siguientes pasos

- Los recordatorios comienzan 12 horas después del registro. Se revisan cada minuto,
  solo con la aplicación abierta. Al reactivar uno se conserva el horario original.
- Los datos se pierden al cerrar; la ventana avisa de esta limitación.
- Faltan persistencia, ordenamiento (RF06), progreso diario (RF08), cuentas reales y
  pruebas con usuarios. Esta versión no debe presentarse como el 85% terminado.
- Se verificó la compilación con Java 17 y pasaron las pruebas automatizadas
  de lógica. La interfaz gráfica queda pendiente de probar en una computadora
  con escritorio; el entorno de preparación no dispone de pantalla gráfica.

## Integrarlo al repositorio del equipo

Trabaja en una rama y conserva el historial del repositorio existente. Este ZIP
no contiene una carpeta `.git` y no sustituye los commits de los integrantes.
Revisa con el equipo los tres archivos anteriores (`InicioSesion.java`,
`RegistrarTareas.java` y `MostrarRecordatorio.java`): su lógica ahora se reparte
entre las clases de `src/`. El programa integrado se inicia con `Main`.
Evita mantener dos implementaciones activas y divergentes; acuerden qué archivos
antiguos retirar cuando hayan probado esta versión.
