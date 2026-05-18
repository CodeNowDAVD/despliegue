# Sesión 08 — CI/CD completo (GOrbitS)

Electivo I — Testing del Software.  
Stack: **Docker** + **Jenkins** + **SonarQube** + **GitHub** + deploy **Termux** (`deploy.sh`).

Checklist imprimible al final de este documento.

---

## Mapa de entregables

| # | Qué pide la sesión | Dónde / cómo |
|---|-------------------|--------------|
| 1 | Red Docker `network_jenkins` | `scripts/sesion08-docker-up.sh` |
| 2 | SonarQube :9001 | Mismo script + UI Sonar |
| 3 | Jenkins :9080 | Mismo script + UI Jenkins |
| 4 | Pipeline Maven (build, test, JaCoCo) | `Jenkinsfile` |
| 5 | Sonar + Quality Gate | Stages Sonar en `Jenkinsfile` |
| 6 | Deploy continuo | Stage Deploy → `ci/deploy-to-termux.sh` |
| 7 | GitHub (token / Actions) | Credencial Jenkins + `.github/workflows/gorbits-ci.yml` |
| 8 | Evidencias | Capturas Jenkins, Sonar, health API |

Deploy manual a Termux (ya lo tienes funcionando): `comandos_despliegue_gorbits.txt`.

---

## Fase 0 — Prerrequisitos (Mac)

- Docker Desktop en marcha
- Java 21 (`java -version`)
- Repo en GitHub (para clone Jenkins y webhook)
- Termux con GOrbitS, MariaDB, Nginx (`192.168.2.4:8022`)

**SSH sin contraseña** (evita el prompt en deploy):

```bash
chmod +x GOrbitS/ci/setup-ssh-termux.sh
./GOrbitS/ci/setup-ssh-termux.sh
```

---

## Fase 1 — Infra Docker (15 min)

Recursos del docente: `~/Downloads/RECURSOS/ci-cd/`

```bash
chmod +x scripts/sesion08-docker-up.sh scripts/sesion08-docker-down.sh
./scripts/sesion08-docker-up.sh
```

| Servicio | URL |
|----------|-----|
| Jenkins | http://localhost:9080 |
| SonarQube | http://localhost:9001 |

**Jenkins — primera vez**

1. Password inicial:
   ```bash
   cat ~/Downloads/RECURSOS/ci-cd/jenkins/jenkins_home/secrets/initialAdminPassword
   ```
2. Instalar plugins sugeridos.
3. Crear usuario admin.

**SonarQube — primera vez**

1. Login `admin` / `admin` → cambiar contraseña.
2. **My Account → Security → Generate Token** → copiar (para Jenkins y local).

---

## Fase 2 — Plugins Jenkins

*Manage Jenkins → Plugins*

Instalar si faltan:

- Maven Integration
- Pipeline
- Git
- SonarQube Scanner
- JaCoCo
- HTML Publisher
- SSH Agent
- Docker Pipeline (opcional)

Reiniciar Jenkins si lo pide.

---

## Fase 3 — Tools y Sonar en Jenkins

**Manage Jenkins → Tools**

| Tool | Nombre en Jenkins | Valor |
|------|-------------------|--------|
| JDK | `JDK21` | JDK 21 (descargar o ruta local) |
| Maven | `MAVEN_HOME` | Maven 3.9.x |

**Manage Jenkins → System → SonarQube servers**

| Campo | Valor |
|-------|--------|
| Name | `sonarqube` |
| Server URL | `http://sonarqube:9000` |

*(Desde el contenedor Jenkins la red `network_jenkins` resuelve el hostname `sonarqube`.)*

**Manage Jenkins → System → SonarQube Scanner**  
→ marcar el servidor `sonarqube` creado arriba.

---

## Fase 4 — Credenciales Jenkins

*Manage Jenkins → Credentials → System → Global*

| ID | Tipo | Uso |
|----|------|-----|
| `github-token` | Username + Password | Usuario GitHub + **Personal Access Token** (scope `repo`) |
| `Sonarqube` | Secret text | Token generado en Sonar |
| `gorbits-ssh` | SSH Username with private key | Usuario `u0_a296` + clave privada (`~/.ssh/id_ed25519`) |

---

## Fase 5 — Job Pipeline GOrbitS

1. **New Item** → `gorbits-pipeline` → **Pipeline**
2. **Pipeline → Definition:** Pipeline script from SCM
3. **SCM:** Git  
   - URL: `https://github.com/TU_USUARIO/despliegueS.git`  
   - Credentials: `github-token`  
   - Branch: `*/main`
4. **Script Path:** `Jenkinsfile`

**Environment del job** (*opciones → Environment variables* o en el job):

| Variable | Valor ejemplo |
|----------|----------------|
| `GIT_REPO_URL` | misma URL del repo |
| `SONAR_HOST_URL` | `http://sonarqube:9000` |
| `DEPLOY_HOST` | `192.168.2.4` |
| `DEPLOY_USER` | `u0_a296` |
| `DEPLOY_SSH_PORT` | `8022` |

---

## Fase 6 — Primer build (sin deploy)

1. **Build with Parameters**
2. `DEPLOY_TO_SERVER` = **false**
3. `RUN_TESTCONTAINERS` = **false** (primera vez; activar después si Jenkins tiene Docker)

Stages esperados en verde:

`Clone` → `Build` → `Test` → `Sonar` → `Quality Gate`

Revisar:

- **JaCoCo GOrbitS** (enlace HTML en el build)
- Sonar: http://localhost:9001 → proyecto **GOrbitS**

### Sonar local (sin Jenkins)

```bash
export SONAR_TOKEN=squ_xxxxxxxx
chmod +x GOrbitS/ci/run-sonar-local.sh
./GOrbitS/ci/run-sonar-local.sh
```

---

## Fase 7 — Deploy automático a Termux

1. Credencial `gorbits-ssh` con la misma clave que `setup-ssh-termux.sh`
2. Mac y Termux en la **misma Wi‑Fi** (`192.168.2.4` accesible desde el host Docker)
3. Build con `DEPLOY_TO_SERVER` = **true**

Si Jenkins no alcanza Termux, prueba en el job:

`DEPLOY_HOST` = IP del Mac en la LAN y reenvío SSH, o ejecuta deploy manual:

```bash
cd GOrbitS && ./ci/deploy-to-termux.sh
```

Health: http://192.168.2.4:8088/api/actuator/health

---

## Fase 8 — GitHub

### A) Subir repo

```bash
cd ~/Programacion/despliegueS
git init   # si aún no es repo
git add Jenkinsfile GOrbitS .github
git commit -m "Sesión 08: CI/CD Jenkins, Sonar, GitHub Actions"
git remote add origin https://github.com/TU_USUARIO/despliegueS.git
git push -u origin main
```

### B) GitHub Actions

Workflow: `.github/workflows/gorbits-ci.yml`  
En cada push a `main` ejecuta `mvn verify` y sube artefacto JaCoCo.

Opcional Sonar en GitHub: Settings → Secrets → `SONAR_TOKEN`, Variables → `SONAR_ENABLED=true`, `SONAR_HOST_URL`.

### C) Webhook Jenkins (opcional)

GitHub → repo → Settings → Webhooks →  
`http://TU_IP_MAC:9080/github-webhook/`  
*(requiere plugin GitHub en Jenkins)*

---

## Fase 9 — Evidencias para el informe

1. Captura `docker ps` (jenkins, sonarqube, sonarqube_db)
2. Captura pipeline Jenkins **SUCCESS** (todos los stages)
3. Captura SonarQube: Overview + Coverage
4. Captura JaCoCo HTML en Jenkins
5. Captura health: `curl http://192.168.2.4:8088/api/actuator/health`
6. (Opcional) GitHub Actions run en verde

---

## Checklist rápido

```
[ ] docker network + sonar + jenkins (sesion08-docker-up.sh)
[ ] Jenkins plugins + JDK21 + Maven
[ ] Credenciales github-token, Sonarqube, gorbits-ssh
[ ] Sonar server «sonarqube» en Jenkins
[ ] Job pipeline from SCM → Jenkinsfile
[ ] Build 1: DEPLOY_TO_SERVER=false → verde hasta Quality Gate
[ ] SSH sin password (setup-ssh-termux.sh)
[ ] Build 2: DEPLOY_TO_SERVER=true → health UP
[ ] Repo en GitHub + workflow Actions
[ ] Capturas para entrega
```

---

## Archivos del proyecto

| Archivo | Rol |
|---------|-----|
| `../Jenkinsfile` | Pipeline Jenkins |
| `ci/deploy-to-termux.sh` | Deploy Termux |
| `ci/setup-ssh-termux.sh` | Clave SSH |
| `ci/run-sonar-local.sh` | Sonar desde Mac |
| `../scripts/sesion08-docker-up.sh` | Levantar Docker |
| `../.github/workflows/gorbits-ci.yml` | CI GitHub |
| `../comandos_despliegue_gorbits.txt` | Operación Termux |

---

## Relación sesiones

| Sesión | Herramienta |
|--------|-------------|
| 07 | Postman — `postman/README.md` |
| 08 | Jenkins + Sonar + deploy |

Ver `TESTING.md`.
