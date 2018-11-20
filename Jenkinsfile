def repoUrl = "git@github.com:Mayvenn/storefront.git"
def appName = 'storefront'

pipeline {
    agent any
    environment {
        PATH = '/sbin:/usr/local/bin/:/usr/sbin:/bin:/usr/bin'
    }

    stages {
        stage('Build') {
            steps {
                runInTest {
                    gitCheckoutWithSubmodules(repoUrl)
                    withLein {
                        sh "docker build -t ${appName}-tests -f Dockerfile.test ."
                    }
                }
            }
        }

        stage('Verify') {
            parallel {
                // stage('Lint') {
                //     steps {
                //         runInTest {
                //             withLein {
                //                 sh "docker run --rm ${appName}-tests lein cljfmt check"
                //             }
                //         }
                //     }
                // }
                stage('No Dup Event Handlers') {
                    steps {
                        runInTest {
                            withLein {
                                sh "docker run --rm ${appName}-tests ./assert_no_duplicate_event_handlers.sh"
                            }
                        }
                    }
                }
                stage('Test') {
                    steps {
                        runInTest {
                            sh "docker rm -f \"${appName}-tests\" || true"
                            withLein {
                                sh "docker run --name \"${appName}-tests\" \"${appName}-tests\""
                            }
                        }
                    }
                    post {
                        always {
                            sh "mkdir -p \"`pwd`/reports/${currentBuild.number}\" || true"
                            sh "docker cp \"${appName}-tests:/app/target/test-reports\" \"`pwd`/reports/${currentBuild.number}\" || true"
                            junit "reports/${currentBuild.number}/**/*.xml"
                        }
                    }
                }
                stage('Dependency Updates') {
                    steps {
                        runInTest {
                            withLein {
                                sh 'lein update-in :plugins conj \'[lein-ancient "0.6.12"]\' -- ancient :all 2>&1 | grep -v \'\\(warn\\)\''
                            }
                        }
                    }
                }
                // TODO(jeff): this is really slow. Is there a way we can speed it up?
                // stage('Check NVD Vulnerabilities') {
                //   steps {
                //     ansiColor('xterm') {
                //       dir('test') {
                //         withCredentials([usernamePassword(credentialsId: 'lein', passwordVariable: 'LEIN_PASSPHRASE', usernameVariable: 'LEIN_USERNAME')]) {
                //           sh 'lein update-in :plugins conj \'[lein-nvd "0.3.1"]\' -- update-in :dependencies conj \'[joda-time/joda-time "2.9.9"]\' -- nvd check'
                //         }
                //       }
                //     }
                //   }
                // }
            }
        }
        stage('Deploy') {
            steps {
                retry(3) {
                    deploy(appName, 'acceptance')
                }
            }
        }
    }
}
