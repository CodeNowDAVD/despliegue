/**
 * Pipeline CI/CD — GOrbitS (Spring Boot JAR, sin Tomcat).
 *
 * Stages: Clone → Build → Test (+ JaCoCo) → Sonar → Quality Gate → Deploy (SSH a vuestro servidor).
 *
 * Credenciales Jenkins (sugeridas):
 *   - github-token     → Git clone
 *   - Sonarqube        → token SonarQube
 *   - gorbits-ssh      → SSH private key para deploy (usuario con permiso en el servidor)
 *
 * Variables de entorno del job (configurar en Jenkins):
 *   - GIT_REPO_URL     → ej. https://github.com/TU_USUARIO/despliegueS.git
 *   - SONAR_HOST_URL   → http://sonarqube:9000 (red Docker) o http://host.docker.internal:9001
 *   - DEPLOY_HOST      → IP Termux (ej. 192.168.2.4)
 *   - DEPLOY_USER      → usuario SSH Termux (ej. u0_a296)
 *   - DEPLOY_SSH_PORT  → puerto SSH Termux (default 8022)
 *
 * Deploy: copia JAR + migraciones y ejecuta ~/servers/gorbits/bin/deploy.sh en Termux.
 * Ver despliegueS/comandos_despliegue_gorbits.txt
 */
pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY_TO_SERVER', defaultValue: false, description: 'Si true, copia JAR + migraciones por SSH y reinicia el servicio')
        booleanParam(name: 'RUN_TESTCONTAINERS', defaultValue: false, description: 'Si true, ejecuta tests @Tag(testcontainers) (requiere Docker en el agente Jenkins)')
        booleanParam(name: 'SKIP_QUALITY_GATE', defaultValue: true, description: 'Si true, no espera Quality Gate (recomendado hasta configurar webhook Sonar→Jenkins)')
    }

    environment {
        SONAR_TOKEN  = credentials('Sonarqube')

        POM_PATH       = 'GOrbitS/pom.xml'
        JAR_FILE       = 'GOrbitS/target/GOrbitS-0.0.1-SNAPSHOT.jar'
        MIGRATIONS_DIR = 'GOrbitS/database/migrations'

        SONAR_HOST_URL = "${env.SONAR_HOST_URL ?: 'http://sonarqube:9000'}"
        DEPLOY_HOST    = "${env.DEPLOY_HOST ?: ''}"
        DEPLOY_USER    = "${env.DEPLOY_USER ?: 'u0_a296'}"
        DEPLOY_SSH_PORT = "${env.DEPLOY_SSH_PORT ?: '8022'}"
        BACKEND_DIR    = "${WORKSPACE}/GOrbitS"
    }

    stages {

        stage('Clone') {
            when {
                expression { env.GIT_REPO_URL?.trim() }
            }
            steps {
                timeout(time: 3, unit: 'MINUTES') {
                    git branch: "${env.BRANCH_NAME ?: 'main'}",
                        credentialsId: 'github-token',
                        url: "${env.GIT_REPO_URL}"
                }
            }
        }

        stage('Check Java 21') {
            steps {
                sh '''
                    echo "JAVA_HOME=${JAVA_HOME:-<no definido>}"
                    java -version 2>&1 | tee /dev/stderr
                    javac -version 2>&1 | tee /dev/stderr
                    java -version 2>&1 | grep -qE 'version "21\\.' || {
                      echo "ERROR: Jenkins debe usar Java 21. Ejecuta: ./scripts/sesion08-jenkins-jdk21-rebuild.sh"
                      exit 1
                    }
                '''
            }
        }

        stage('Build') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    sh 'cd GOrbitS && chmod +x mvnw && ./mvnw -B clean package -DskipTests'
                }
            }
        }

        stage('Test') {
            environment {
                DOCKER_HOST                   = 'unix:///var/run/docker.sock'
                TESTCONTAINERS_RYUK_DISABLED  = 'true'
                TESTCONTAINERS_CHECKS_DISABLE = 'true'
                TESTCONTAINERS_HOST_OVERRIDE  = 'host.docker.internal'
            }
            steps {
                timeout(time: 15, unit: 'MINUTES') {
                    script {
                        def tcFlag = params.RUN_TESTCONTAINERS ? '-DexcludedGroups=' : ''
                        sh "cd GOrbitS && chmod +x mvnw && ./mvnw -B clean verify ${tcFlag}"
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'GOrbitS/target/surefire-reports/*.xml'
                    // JaCoCo HTML: ver artefacto GOrbitS/target/site/jacoco/ o instalar plugin "HTML Publisher"
                }
            }
        }

        stage('Sonar') {
            steps {
                timeout(time: 8, unit: 'MINUTES') {
                    withSonarQubeEnv('sonarqube') {
                        sh '''
                            cd GOrbitS && chmod +x mvnw
                            ./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar \
                                -Dsonar.token="${SONAR_TOKEN}" \
                                -Dsonar.host.url="${SONAR_HOST_URL}"
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            when {
                expression { !params.SKIP_QUALITY_GATE }
            }
            steps {
                sleep(time: 10, unit: 'SECONDS')
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy to Termux') {
            when {
                allOf {
                    expression { params.DEPLOY_TO_SERVER }
                    expression { env.DEPLOY_HOST?.trim() }
                }
            }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    sshagent(credentials: ['gorbits-ssh']) {
                        sh """
                            chmod +x GOrbitS/ci/deploy-to-termux.sh
                            BACKEND_DIR='${BACKEND_DIR}' \\
                            TERMUX_USER='${DEPLOY_USER}' \\
                            TERMUX_HOST='${DEPLOY_HOST}' \\
                            TERMUX_PORT='${DEPLOY_SSH_PORT}' \\
                            GOrbitS/ci/deploy-to-termux.sh
                        """
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                if (params.DEPLOY_TO_SERVER && env.DEPLOY_HOST?.trim()) {
                    echo "Deploy OK → http://${DEPLOY_HOST}:8088/api/actuator/health (Nginx → GOrbitS)"
                } else {
                    echo "Pipeline OK — artefacto: ${JAR_FILE}"
                }
            }
        }
        failure {
            echo 'Pipeline falló — revisar Test, Sonar Quality Gate o Deploy en la consola Jenkins.'
        }
        always {
            archiveArtifacts artifacts: 'GOrbitS/target/*.jar,GOrbitS/target/site/jacoco/**', allowEmptyArchive: true
            cleanWs()
        }
    }
}
