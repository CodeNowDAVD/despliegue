# Pruebas automatizadas — GOrbitS

Guía de testing alineada con el curso de **Testing del Software**: pruebas unitarias (Mockito), integración API (MockMvc + H2), capa repository (Testcontainers + MariaDB) y cobertura (JaCoCo).

## Tipos de pruebas en el proyecto

| Tipo | Ubicación | Tecnología | Base de datos |
|------|-----------|------------|---------------|
| **Unitarias** | `src/test/java/biblioteca/gorbits/unit/` y otros `*Test.java` (sin `Api`) | JUnit 5, Mockito, AssertJ | Ninguna (mocks) |
| **Integración API** | `src/test/java/biblioteca/gorbits/**/*ApiTest.java` | `@SpringBootTest`, MockMvc, Spring Security | H2 en memoria (perfil `test`) |
| **Repository** | `BookCategoryRepositoryTest` | `@DataJpaTest`, Testcontainers | MariaDB 11 en Docker (perfil `tc`) |
| **Smoke** | `GOrbitSApplicationTests` | Contexto Spring completo | H2 (perfil `test`) |

### Pruebas unitarias (Mockito)

- Anotaciones: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `@BeforeEach`, `@Test`, `@ParameterizedTest`.
- Verifican lógica de servicios y reglas **sin** levantar Spring ni base de datos.
- Ejemplos canónicos en `unit/`:
  - `unit/commercial/GuideLifecycleRulesUnitTest` — reglas puras + casos parametrizados.
  - `unit/catalog/CatalogServiceUnitTest` — éxito, duplicado, lista vacía, `verify(times(1))`.
  - `unit/billing/BillingServiceUnitTest` — totales, excepciones, validaciones.

Helpers: `testsupport/UnitTestFixtures.java`.

### Pruebas de integración API

- Levantan el contexto completo (`@SpringBootTest`, `@ActiveProfiles("test")`).
- HTTP real contra controladores con `MockMvc` y JWT.
- Configuración: `src/test/resources/application-test.properties`.

### Pruebas de repository (Testcontainers)

- Clase: `catalog/BookCategoryRepositoryTest`.
- Contenedor compartido: `config/MariaDBTestContainer.java` (imagen `mariadb:11.4`).
- Propiedades dinámicas: `@DynamicPropertySource` + `application-tc.properties`.
- Etiqueta JUnit: `@Tag("testcontainers")` — requiere **Docker** en ejecución.

## Requisitos

- **Java 21**
- **Maven Wrapper** (`./mvnw`)
- **Docker** (solo para tests Testcontainers; opcional si se excluye el grupo)

## Comandos

### Pruebas habituales (sin Docker)

Por defecto **no** se ejecutan los tests `@Tag("testcontainers")` (repository con MariaDB):

```bash
cd GOrbitS
./mvnw test
```

### Incluir Testcontainers (requiere Docker en ejecución)

```bash
./mvnw test -Dsurefire.excludedGroups=
```

### Solo pruebas unitarias (rápido, sin Spring)

```bash
./mvnw test -Dtest='**/unit/**/*Test,**/*Test,!**/*ApiTest,!GOrbitSApplicationTests,!**/BookCategoryRepositoryTest'
```

### Cobertura JaCoCo

```bash
./mvnw clean test jacoco:report
```

Reportes generados:

- HTML: `target/site/jacoco/index.html`
- XML (SonarQube): `target/site/jacoco/jacoco.xml`

## Perfiles de test

| Perfil | Archivo | Uso |
|--------|---------|-----|
| `test` | `application-test.properties` | Integración API, H2 memoria |
| `tc` | `application-tc.properties` | `@DataJpaTest` + Testcontainers |

No se usan credenciales ni hosts de producción en ningún perfil de test.

## Cobertura JaCoCo

Tras `./mvnw test jacoco:report`, abrir `target/site/jacoco/index.html`.

El informe **excluye** del cómputo (código sin lógica de negocio o arranque):

- `GOrbitSApplication`, seeds demo H2, migraciones H2 legacy
- Paquetes `**/dto/**` (records de transporte)

Objetivo del proyecto: **≥ 85 %** en instrucciones sobre servicios/controladores. Para acercarse al 100 % en el reporte global, ejecutar la suite completa con Docker y revisar paquetes `inventory` y `commercial` (servicios grandes).

```bash
./mvnw clean test -Dsurefire.excludedGroups= jacoco:report
```

## Sesión 08 — CI/CD (Jenkins + SonarQube + deploy servidor)

Guía completa paso a paso: **`CI.md`** (checklist, Docker, Jenkins UI, Sonar, GitHub Actions, evidencias).

Inicio rápido infra: `../scripts/sesion08-docker-up.sh` → Jenkins :9080, Sonar :9001.

## Sesión 07 — Pruebas de integración (Postman)

Colección y entornos en `postman/`:

- `GOrbitS-Sesion07-Integracion.postman_collection.json`
- `GOrbitS-Dev-H2.postman_environment.json`
- `GOrbitS-Prod-MariaDB.postman_environment.json`

Instrucciones detalladas: `postman/README.md`.

1. Levantar API: `./mvnw spring-boot:run` (H2) o JAR con `mysql,prod`.
2. Importar en Postman → entorno **GOrbitS Dev (H2)**.
3. **Collection Runner** en orden de carpetas `0`–`5`.
4. Comparar resultados dev (H2) vs servidor MariaDB con el segundo entorno.

Los requests cubren JWT, flujos guía/cuotas/pagos y reglas `GuideLifecycleRules` (401, 403, 404, 409).

## Evidencia para el manual / informe

1. Captura de `./mvnw test` con **BUILD SUCCESS** y resumen de tests.
2. Captura del **Postman Collection Runner** (Sesión 07) con tests en verde.
3. Captura de `target/site/jacoco/index.html` (porcentaje de cobertura).
4. Fragmento de una clase en `unit/` mostrando `@Mock`, `@InjectMocks` y `verify`.
5. Fragmento de `BookCategoryRepositoryTest` mostrando `@DataJpaTest` y `@DynamicPropertySource`.
6. (Opcional) Salida de `docker ps` con contenedor MariaDB durante los tests repository.

## Estructura resumida

```
src/test/java/biblioteca/gorbits/
├── unit/                    # Unitarias canónicas (curso)
├── catalog/
│   ├── CatalogServiceTest
│   └── BookCategoryRepositoryTest   # Testcontainers
├── config/
│   └── MariaDBTestContainer.java
├── **/*ApiTest.java         # Integración API
└── testsupport/
    └── UnitTestFixtures.java

src/test/resources/
├── application-test.properties
└── application-tc.properties
```
