def repoUrl = "git@github.com:Mayvenn/storefront.git"
def appName = 'storefront'

def runIn(Closure body) {
    ansiColor('xterm') {
        dir('test') {
            body()
        }
    }
}

def gitCheckoutWithSubmodules(String gitURL) {
    checkout([$class: 'GitSCM',
              branches: [[name: '*/master']],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'SubmoduleOption',
                            disableSubmodules: false,
                            parentCredentials: false,
                            recursiveSubmodules: true,
                            reference: '',
                            trackingSubmodules: false]],
              submoduleCfg: [],
              userRemoteConfigs: [[url: gitURL]]])
}

def withLein(Closure body) {
    withCredentials([usernamePassword(credentialsId: 'lein', passwordVariable: 'LEIN_PASSPHRASE', usernameVariable: 'LEIN_USERNAME')]) {
        body()
    }
}

def deploy(String app, String env) {
    ansiColor('xterm') {
        dir('rainman') {
            gitCheckoutWithSubmodules('git@github.com:Mayvenn/rainman.git')
            withCredentials([usernamePassword(credentialsId: 'docker', passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
                sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
            }
            sh 'bundle check --path ~/rainman-gems || bundle --path ~/rainman-gems --retry 3 --jobs 4'
            withCredentials([usernamePassword(credentialsId: 'aws', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
                withLein {
                    withCredentials([string(credentialsId: 'envvarspassword', variable: 'ENV_VARS_PASSWORD')]) {
                        sh "bundle exec ./rain ${app} ${env}"
                    }
                }
            }
        }
    }
}

pipeline {
    agent any
    environment {
        PATH = '/sbin:/usr/local/bin/:/usr/sbin:/bin:/usr/bin'
    }

    stages {
        stage('Build') {
            steps {
                runIn {
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
                //         runIn {
                //             withLein {
                //                 sh "docker run --rm ${appName}-tests lein cljfmt check"
                //             }
                //         }
                //     }
                // }
                stage('No Dup Event Handlers') {
                    steps {
                        runIn {
                            withLein {
                                sh "docker run --rm ${appName}-tests ./assert_no_duplicate_event_handlers.sh"
                            }
                        }
                    }
                }
                stage('Test') {
                    steps {
                        runIn {
                            withLein {
                                sh "docker run --rm -v `pwd`/target/test-reports:/app/target/test-reports ${appName}-tests"
                            }
                        }
                    }
                    post {
                        always {
                            junit 'test/target/test-reports/**/*.xml'
                        }
                    }
                }
                stage('Dependency Updates') {
                    steps {
                        runIn {
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
