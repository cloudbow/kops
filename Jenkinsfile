podTemplate(
    label: 'pod1',
    containers:
        [
            // TODO: build custom containers that have everything we need
            containerTemplate(name: 'builder-docker', image: 'registry.sports-cloud.com:5000/builder-docker:latest', command: 'cat', ttyEnabled: true),
            containerTemplate(name: 'docker', image: 'docker', ttyEnabled: true, command: 'cat'),
            containerTemplate(name: 'helm', image: 'lachlanevenson/k8s-helm:latest', command: 'cat', ttyEnabled: true)
        ],
        volumes:
        [
            hostPathVolume(mountPath: '/var/run/docker.sock', hostPath: '/var/run/docker.sock'),
            hostPathVolume(mountPath: '/root/.m2', hostPath: '/root/.m2'),
            hostPathVolume(mountPath: '/root/.ivy2', hostPath: '/root/.ivy2')
        ]
)

{
    node('pod1')
    {
      if (env.TAG_NAME =~ "^docker/.*" || env.TAG_NAME =~ "^docker-dev/.*") {
            TAG_TO_RUN = env.TAG_NAME
            REAL_VERSION=sh(returnStdout: true, script: "echo ${TAG_TO_RUN} | cut -f2 -d '/' | cut -f2 -d '_'").trim()
            //validate version
            if((env.TAG_NAME =~ "^docker-dev/.*" && !(REAL_VERSION =~ "alpha")) ||
               (env.TAG_NAME =~ "^docker/.*" && !(REAL_VERSION =~ "alpha") )
             ) {
                slackSend channel: '#jenkins-builds', color: 'bad', message: "Need to supply the right version.Namespaced tags docker-dev/* should contain alpha in version. Namespaced tags with docker/* should not contain alpha versions "
            } else {
                stage('Build and Push image')
                {

                    def ENV = input(
                            id: 'userInput', message: 'The environment to burn the image?', parameters: [
                            [$class: 'TextParameterDefinition', defaultValue: 'dev', description: 'Environment', name: 'env']
                    ])
                    container('builder-docker')
                    {
                        // Get the code
                        sourceControl = checkout scm
                        
                        PROJECT_DIR=pwd()
                        echo "Working on TAG ${TAG_TO_RUN}"
                        try{
                            sh """ 
                                   cd kubernetes && \
                                   make INPUT_TAG=${TAG_TO_RUN} BASE_PATH=${PROJECT_DIR}/kubernetes/${ENV} add-docker-artifacts-from-project-for-tag
                               """
                            echo "Got the tag ${TAG_TO_RUN}"
                            slackSend channel: '#jenkins-builds', color: 'good', message: "Building image ${TAG_TO_RUN} in env ${ENV}"
                            container('docker')
                            {
                                
                                sh """
                                       apk update && \
                                       apk add make && \
                                       cd kubernetes && \
                                       make DOCKER_ID="registry.sports-cloud.com:5000" INPUT_TAG=${TAG_TO_RUN} ENV=${ENV} BASE_PATH=${PROJECT_DIR}/kubernetes/${ENV} build-and-push-docker-container-for-tag   
                                   """
                                slackSend channel: '#jenkins-builds', color: 'good', message: "Built image for ${TAG_TO_RUN} in env ${ENV}"
                       
                            }
                        }
                        catch(error) {
                             slackSend channel: '#jenkins-builds', color: 'bad', message: "Failed to build image for ${TAG_TO_RUN} in env ${ENV}"
                             error "Failed to build image. Please see logs"
                        }
                    }

                }
            }
        }

        if (env.TAG_NAME =~ "^release/.*" || env.TAG_NAME =~ "^dev/.*" || env.TAG_NAME =~ "^qa/.*" || env.BRANCH_NAME =~ "^feature/.*") {
            stage('Deploy Image')
            {

                def ENV = input(
                        id: 'userInput', message: 'The environment to deploy the image?', parameters: [
                        [$class: 'TextParameterDefinition', defaultValue: 'dev', description: 'Environment', name: 'env']
                ])
                container('helm')
                {
                    
                    // Get the code
                    sourceControl = checkout scm
                    PWD=pwd()
                    echo "Working on directory ${PWD}"
                    TAG_TO_RUN = env.TAG_NAME
                    BRANCH_TO_RUN=env.BRANCH_NAME
                    REAL_VERSION=sh(returnStdout: true, script: "echo ${TAG_TO_RUN} | cut -f2 -d '/'  | sed 's/v//g'").trim()  
                    sh """
                            apk update && \
                            apk add gawk && \
                            apk add git
                       """
                    LAST_COMMIT_LINE=sh(returnStdout: true, script: "git log -1 --format='%b' | awk 'NF{s=\$0}END{print s}'").trim()  
                    echo "Last commit ${LAST_COMMIT_LINE}"
                    try{            
                        slackSend channel: '#jenkins-builds', color: 'good', message: "Deploying app for tag ${REAL_VERSION} or branch ${BRANCH_TO_RUN} "
                        sh """                      
                              helm init && \
                              helm upgrade --install -f ${PWD}/config/charts/${ENV}/values.yaml sc-apps-${ENV} ${PWD}/kubernetes/charts/repo/slingtv-sports-cloud-apps --version ${REAL_VERSION}
                           """
                        slackSend channel: '#jenkins-builds', color: 'good', message: "Deployed sports cloud app for version ${REAL_VERSION} or branch ${BRANCH_TO_RUN}"
                        slackSend channel: '#jenkins-builds', color: 'good', message: "Last commit line - ${LAST_COMMIT_LINE}"
                    }
                    catch(error) {
                         slackSend channel: '#jenkins-builds', color: 'bad', message: "Failed to deploy app for tag ${TAG_TO_RUN} or branch ${BRANCH_TO_RUN}"
                         error "Failed to deploy app. Please see logs"
                    }

                }


            }
        }
    }
}
