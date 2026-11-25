# Auto Anchor (Fabric 1.21.11)

Mod de cliente. Cuando esta ACTIVADO, tienes un totem en la mano principal y miras un
ancla de reaparicion cargada (fuera del Nether), interactua con ella automaticamente.

- Tecla por defecto: G (cambiala en Opciones > Controles > Auto Anchor). Empieza DESACTIVADO.
- Requiere Fabric Loader 0.18.1+ y Fabric API para 1.21.11.

## Compilar con GitHub (sin PC potente)
1. Crea un repositorio en github.com y sube TODO el contenido de esta carpeta (incluida .github).
2. Ve a la pestana Actions > "build" > espera a que termine (check verde).
3. Abre la ejecucion y descarga el artefacto "autoanchor-mod" (zip con el .jar).
   Usa el .jar que NO termina en -sources.

## Compilar en PC
Instala Java 21 y Gradle 9.2.1, luego: gradle build  (el .jar queda en build/libs/).

Si falla, copia el error y pegaselo a Claude.
