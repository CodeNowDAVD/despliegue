/**
 * Pipeline CI/CD — GOrbitS (Spring Boot JAR → Termux, sin Tomcat).
 *
 * Credenciales Jenkins:
 *   - github-token  → clone (opcional si el job ya usa SCM)
 *   - Sonarqube     → token SonarQube
 *   - gorbits-ssh   → clave SSH deploy (usuario Termux)
 *
 * Variables globales en Jenkins (Manage Jenkins → System):
 *   DEPLOY_HOST, DEPLOY_USER, DEPLOY_SSH_PORT
 *
 * Setup completo: ./scripts/sesion08-finish-setup.sh
 */
pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY_TO_SERVER', defaultValue: false,
            description: 'Si true, copia JAR + migraciones por SSH y reinicia el servicio')
        booleanParam(name: 'RUN_TESTCONTAINERS', defaultValue: false,
            description: 'Tests @Tag(testcontainers); requiere Docker en el agente Jenkins')
        booleanParam(name: 'SKIP_QUALITY_GATE', defaultValue: false,
            description: 'Si true, no espera Quality Gate (-Dsonar.qualitygate.wait)')
    }

    environment {
        SONAR_TOKEN     = credentials('Sonarqube')
        GIT_REPO_URL    = "${env.GIT_REPO_URL ?: 'https://github.com/CodeNowDAVD/despliegue.git'}"
        DEPLOY_HOST     = "${env.DEPLOY_HOST ?: '192.168.2.4'}"
        DEPLOY_USER     = "${env.DEPLOY_USER ?: 'u0_a296'}"
        DEPLOY_SSH_PORT = "${env.DEPLOY_SSH_PORT ?: '8022'}"
        POM_PATH        = 'GOrbitS/pom.xml'
        JAR_FILE        = 'GOrbitS/target/GOrbitS-0.0.1-SNAPSHOT.jar'
        BACKEND_DIR     = "${WORKSPACE}/GOrbitS"
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
                    java -version 2>&1 | tee /dev/stderr
                    java -version 2>&1 | grep -qE 'version "21\\.' || {
                      echo "ERROR: Jenkins necesita JDK 21. Ejecuta: ./scripts/sesion08-jenkins-rebuild.sh"
                      exit 1
                    }
                    command -v ssh >/dev/null || {
                      echo "ERROR: falta openssh-client en la imagen Jenkins. Ejecuta: ./scripts/sesion08-jenkins-rebuild.sh"
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
                }
            }
        }

        stage('Sonar & Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    script {
                        // Maven espera el Quality Gate en el scanner (más fiable que waitForQualityGate en Jenkins)
                        def qgWait = params.SKIP_QUALITY_GATE ? '' : '-Dsonar.qualitygate.wait=true'
                        withSonarQubeEnv('sonarqube') {
                            sh """
                                cd GOrbitS && chmod +x mvnw
                                ./mvnw -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.11.0.3922:sonar ${qgWait}
                            """
                        }
                    }
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
                    withCredentials([sshUserPrivateKey(
                        credentialsId: 'gorbits-ssh',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_DEPLOY_USER')]) {
                        sh """
                            chmod +x GOrbitS/ci/deploy-to-termux.sh GOrbitS/ci/deploy-via-host-network.sh
                            export SSH_KEY_FILE="\${SSH_KEY_FILE}"
                            export BACKEND_DIR='${BACKEND_DIR}'
                            export TERMUX_USER="\${SSH_DEPLOY_USER:-${DEPLOY_USER}}"
                            export TERMUX_HOST='${DEPLOY_HOST}'
                            export TERMUX_PORT='${DEPLOY_SSH_PORT}'
                            # En Mac: antes del build ejecutar ./scripts/termux-ssh-tunnel.sh
                            if [ -f /.dockerenv ]; then
                              export JENKINS_DEPLOY_HOST=host.docker.internal
                              export JENKINS_DEPLOY_PORT=28022
                            fi
                            GOrbitS/ci/deploy-via-host-network.sh
                        """
                    }
                }
            }
            post {
                success {
                    sh """
                        sleep 5
                        curl -sf --connect-timeout 15 \\
                            "http://${DEPLOY_HOST}:8088/api/actuator/health" \\
                            || echo "AVISO: health no respondió (¿Nginx/GOrbitS arrancando?)"
                    """
                }
            }
        }
    }

    post {
        success {
            script {
                if (params.DEPLOY_TO_SERVER && env.DEPLOY_HOST?.trim()) {
                    echo "Deploy OK → http://${DEPLOY_HOST}:8088/api/actuator/health"
                } else {
                    echo "Pipeline OK — artefacto: ${JAR_FILE}"
                }
            }
        }
        failure {
            echo 'Pipeline falló — revisar consola: Test, Sonar Quality Gate o Deploy SSH.'
        }
        always {
            archiveArtifacts artifacts: 'GOrbitS/target/*.jar,GOrbitS/target/site/jacoco/**',
                              allowEmptyArchive: true
        }
    }
}
