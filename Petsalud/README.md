# Pruebas unitarias de PETSALUD

Actividad PBY2202 · Experiencia 2 · Semana 4 · Forma B.

## Ejecución

1. Instalar JDK 17 e importar el archivo `pom.xml` como proyecto Maven.
2. Ejecutar `.\mvnw.cmd clean verify` en Windows o `sh ./mvnw clean verify` en Linux y macOS.
3. Abrir `target/site/jacoco/index.html`. El umbral es 80 % de líneas para cada una de las clases UsuarioServiceImpl, VentaServiceImpl y DatosUsuario. Los resultados de Surefire quedan en `target/surefire-reports`.

Las pruebas unitarias usan repositorios simulados con Mockito. La prueba de contexto utiliza H2 en memoria. La primera ejecución requiere acceso a Maven Central si las dependencias aún no están en caché.

## Publicación

Extraer el proyecto, crear un repositorio en GitHub y publicar el contenido de esta carpeta como raíz del repositorio. Copiar el enlace al informe y al campo Entrega del AVA. Las salidas temporales de Maven quedan excluidas mediante `.gitignore`.
