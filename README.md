# Gestor Académico

Este proyecto lo desarrollamos William Reyes, Marlon Yaeggy y Santiago Escobar para el curso de Programación Orientada a Objetos.

El programa permite organizar tareas, proyectos y exámenes, asignar prioridades y consultar el progreso diario. Está desarrollado en Java con una interfaz Swing y guarda las actividades en SQLite.

## Cómo abrir el programa

Se necesita tener instalado un JDK 17 o superior. Primero hay que extraer el ZIP completo y abrir la carpeta `GestorAcademico` en VS Code.

La terminal debe estar dentro de la carpeta que contiene `src`, `bin`, `lib` y `data`. Si todavía estás en `GestorAcademico-SQLite`, entra con:

```powershell
cd GestorAcademico
```

Después compila el proyecto:

```powershell
javac -encoding UTF-8 -cp "lib/*" -d bin src/*.java tests/*.java
```

Si es la primera vez que lo abres, ejecuta el instalador de las librerías. Este paso necesita Internet:

```powershell
java -cp bin InstalarSQLite
```

Cuando aparezca “Dependencias listas”, puedes comprobar la conexión con SQLite:

```powershell
java --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" PruebasSQLite
```

Si las pruebas terminan correctamente, abre el programa:

```powershell
java --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" Main
```

En la versión corregida, el usuario de demostración es `william` y la contraseña es `12345`. Después de tres intentos incorrectos hay que cerrar y volver a abrir el programa.

Para las siguientes veces basta con ejecutar el último comando. Solo necesitas recompilar si modificas el código.

## Dónde se guarda la información

Las actividades se guardan automáticamente en `data/actividades.db`. No hay que instalar un servidor ni mantener Internet conectado para usar la agenda.

No borres la carpeta `data` si quieres conservar tus actividades. Los recordatorios funcionan mientras la aplicación está abierta.

## Carpetas

- `src`: código fuente Java.
- `bin`: clases compiladas para ejecutar el programa localmente.
- `lib`: librerías necesarias para SQLite.
- `data`: base de datos.
- `assets`: logo de la UVG.
- `tests`: pruebas del programa.

La carpeta `bin` está excluida de GitHub mediante `.gitignore`.

Repositorio: https://github.com/santidrox/Proyecto-Progra
