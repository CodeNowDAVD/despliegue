/**
 * Pipeline CI/CD — GOrbitS (Sesión 08).
 *
 * Deploy: POST HTTPS → https://app.gorbits.xyz/deploy (Bearer token)
 * Equivalente a Tomcat Manager, pero con JAR + Deploy Receiver.
 *
 * Credenciales Jenkins:
 *   - github-token         → Git clone (opcional)
 *   - Sonarqube            → token SonarQube
 *   - gorbits-deploy-token → Secret text, token Bearer del servidor
 *
 * Infra: ./scripts/sesion08-docker-up.sh
 */
pipeline {
    agent any

    parameters {
        booleanParam(name: 'DEPLOY_TO_SERVER', defaultValue: false,
            description: 'Si true, envía JAR a https://app.gorbits.xyz/deploy')
        booleanParam(name: 'RUN_TESTCONTAINERS', defaultValue: false,
            description: 'Tests @Tag(testcontainers); requiere Docker en el agente')
        booleanParam(name: 'SKIP_QUALITY_GATE', defaultValue: false,
            description: 'Si true, no espera Quality Gate (-Dsonar.qualitygate.wait)')
    }

    environment {
        SONAR_TOKEN   = credentials('Sonarqube')
        GIT_REPO_URL  = "${env.GIT_REPO_URL ?: 'https://github.com/CodeNowDAVD/despliegue.git'}"
        DEPLOY_URL    = "${env.DEPLOY_URL ?: 'https://app.gorbits.xyz/deploy'}"
        HEALTH_URL    = "${env.HEALTH_URL ?: 'https://app.gorbits.xyz/api/actuator/health'}"
        JAR_FILE      = 'GOrbitS/target/GOrbitS-0.0.1-SNAPSHOT.jar'
        BACKEND_DIR   = "${WORKSPACE}/GOrbitS"
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
                    java -version 2>&1 | grep -qE 'version "21\\.' || {
                      echo "ERROR: Jenkins necesita JDK 21"
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

        stage('Deploy Backend') {
            when {
                expression { params.DEPLOY_TO_SERVER }
            }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    withCredentials([string(
                        credentialsId: 'gorbits-deploy-token',
                        variable: 'DEPLOY_TOKEN')]) {
                        sh '''
                            chmod +x GOrbitS/ci/deploy-http-gorbits.sh
                            export BACKEND_DIR="${WORKSPACE}/GOrbitS"
                            export DEPLOY_URL="${DEPLOY_URL}"
                            export HEALTH_URL="${HEALTH_URL}"
                            GOrbitS/ci/deploy-http-gorbits.sh
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                if (params.DEPLOY_TO_SERVER) {
                    echo "Deploy HTTP OK → ${env.DEPLOY_URL}"
                    echo "Health → ${env.HEALTH_URL}"
                } else {
                    echo "Pipeline OK — artefacto: ${JAR_FILE}"
                }
            }
        }
        failure {
            echo 'Pipeline falló — revisar Test, Sonar o Deploy HTTP (/deploy).'
        }
        always {
            archiveArtifacts artifacts: 'GOrbitS/target/*.jar,GOrbitS/target/site/jacoco/**',
                              allowEmptyArchive: true
        }
    }
}
