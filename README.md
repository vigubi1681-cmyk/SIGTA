# SIGTA – Sistema web de solicitudes de la Municipalidad Distrital de Chorrillos

Proyecto académico del curso Soluciones Web y Aplicaciones Distribuidas.
No es un canal oficial de la Municipalidad.

## Requisitos
- Java 21, MySQL 8, Maven (incluido como `mvnw`)

## Cómo ejecutar el backend
1. En MySQL Workbench ejecutar, en orden, `database/SIGTA_MySQL_8.sql` y `database/SIGTA_procedimientos.sql`.
2. Crear un usuario de MySQL con permisos sobre `sigta_chorrillos`.
3. Copiar `sigta-backend/src/main/resources/application-local.properties.example` a `application-local.properties` y completar los valores.
4. Dentro de `sigta-backend`: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`
5. La API queda en http://localhost:8080 (el administrador inicial se crea al arrancar).

## Integrantes Grupo 9
- Víctor Ernesto Gutiérrez Birot 
- Renzo Enrique Lara Condori 
- Karen Narciso Checasaca 
- Ana Gabriela Silva Yauyo