# Documentación del Proyecto - Microservicio de Servicios y Conectividad (GestoPago Integration)

Este repositorio contiene el código fuente de un microservicio desarrollado en **Spring Boot**[cite: 5], diseñado para gestionar operaciones de clientes, cuentas, validaciones de *onboarding*[cite: 5], y la integración robusta mediante **Feign Clients** con servicios externos (como **GestoPago**)[cite: 5].

---

## 🚀 1. Tecnologías y Stack Utilizado

* **Java** (Versión configurada para entornos modernos con Gradle)[cite: 5]
* **Spring Boot** (Framework principal para la creación de APIs REST y gestión de dependencias)[cite: 5]
* **Spring Cloud OpenFeign** (Para la comunicación declarativa y segura entre microservicios)[cite: 5]
* **Spring Data JPA / Hibernate** (Mapeo objeto-relacional y persistencia de datos)[cite: 5]
* **Flyway** (Control de versiones y migraciones de base de datos)[cite: 5]
* **Bean Validation** (Validación estricta de DTOs y reglas de negocio en peticiones de entrada)[cite: 5]
* **JUnit 5 & Mockito** (Pruebas unitarias y de integración exhaustivas)[cite: 5]
* **OpenAPI / Swagger** (Documentación interactiva de la API, configurada con soporte para estrategias de *forward-headers* para HTTPS en despliegues como Railway)[cite: 5]

---

## 📂 2. Arquitectura del Proyecto

El proyecto sigue una arquitectura en capas limpia y desacoplada, organizada bajo el paquete base `com.proyecto.servicios`[cite: 5]:

* **`controller/`**: Controladores REST encargados de exponer los endpoints de clientes, personas y productos GestoPago, manejando los códigos de estado HTTP correctos (200, 201, 204, 400, 404, 409)[cite: 5].
* **`service/` y `service/Impl/`**: Capa de lógica de negocio que orquesta las reglas operativas, validaciones de unicidad, mapeos y consumo de clientes externos[cite: 5].
* **`repositorys/`**: Interfaces de Spring Data JPA para la interacción eficiente con la base de datos relacional[cite: 5].
* **`entity/`**: Clases de persistencia y modelos de dominio estructurados (incluyendo módulos para clientes, cuentas, domicilios y tokens de integración)[cite: 5].
* **`client/`**: Clientes declarativos Feign orientados a la integración con pasarelas externas (autenticación y catálogo de productos GestoPago)[cite: 5].
* **`config/`**: Clases de configuración global (seguridad, migraciones Flyway, beans de Spring, OpenAPI e interceptores Feign)[cite: 5].
* **`exception/`**: Manejador global de excepciones (`GlobalExceptionHandler`) que captura errores personalizados (como registros duplicados, recursos no encontrados o fallos de integración) y estandariza las respuestas de error[cite: 5].
* **`model/` y `mapper/`**: Objetos de transferencia de datos (DTOs con validaciones estrictas para *onboarding*) y convertidores de entidades a modelos utilizando componentes seguros[cite: 5].

---

## ⚙️ 3. Características Principales y Configuración

* **Gestión Integral de Clientes**: Registro, actualización, consultas avanzadas (por CURP, RFC, correo, rangos de fecha) y aplicación de bajas lógicas[cite: 5].
* **Validaciones de Entrada Robustas**: Los DTOs de registro validan estrictamente campos obligatorios, formatos de correo, CURP, RFC, códigos postales y números telefónicos móviles[cite: 5].
* **Seguridad y Conectividad con GestoPago**: Implementación de interceptores dedicados para la inyección automática de tokens de autenticación en las peticiones HTTP realizadas mediante Feign[cite: 5].
* **Soporte para Despliegues en la Nube (Railway)**: Configuración optimizada de cabeceras (`forward-headers-strategy`) para garantizar que la documentación interactiva de **Swagger/OpenAPI** renderice correctamente bajo protocolos HTTPS en producción[cite: 5].

---

## 🧪 4. Pruebas (Testing)

El microservicio cuenta con un alto porcentaje de cobertura de pruebas automatizadas ubicadas en el directorio `src/test`:
* **Pruebas Unitarias**: Validación de controladores, servicios, mapeadores y restricciones de validación en DTOs[cite: 5].
* **Pruebas de Integración (`ClienteRepositoryIntegrationTest`)**: Verificación del comportamiento de repositorios, restricciones de unicidad en base de datos y consultas complejas[cite: 5].

Para ejecutar las pruebas y generar los reportes de calidad, utiliza el siguiente comando de Gradle[cite: 5]:
```bash
./gradlew test
