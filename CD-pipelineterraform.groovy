pipeline {
    agent any
    
    environment {
        ACR_NAME = "myacr"  // Azure Container Registry Name
        IMAGE_NAME = "myapp" // Docker Image Name
        ACR_LOGIN_SERVER = "myacr.azurecr.io" // ACR Login Server
        KUBE_NAMESPACE = "default"  // Kubernetes Namespace
        DEPLOYMENT_NAME = "myapp-deployment"  // Kubernetes Deployment Name
        KUBE_CONFIG_CRED_ID = "kubeconfig"  // Jenkins credential ID for Kubernetes config
    }

    stages {
        stage('Checkout Code') {
            steps {
                git 'https://github.com/myrepo/myapp.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${ACR_LOGIN_SERVER}/${IMAGE_NAME}:latest", "-f Dockerfile .")
                }
            }
        }

        stage('Login to ACR') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'azure-acr-credentials', usernameVariable: 'ACR_USERNAME', passwordVariable: 'ACR_PASSWORD')]) {
                        sh "echo $ACR_PASSWORD | docker login $ACR_LOGIN_SERVER -u $ACR_USERNAME --password-stdin"
                    }
                }
            }
        }

        stage('Push Image to ACR') {
            steps {
                script {
                    docker.image("${ACR_LOGIN_SERVER}/${IMAGE_NAME}:latest").push()
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withKubeConfig([credentialsId: KUBE_CONFIG_CRED_ID, serverUrl: 'https://myaks-cluster-url']) {
                        sh """
                        kubectl set image deployment/${DEPLOYMENT_NAME} ${IMAGE_NAME}=${ACR_LOGIN_SERVER}/${IMAGE_NAME}:latest -n ${KUBE_NAMESPACE}
                        kubectl rollout status deployment/${DEPLOYMENT_NAME} -n ${KUBE_NAMESPACE}
                        """
                    }
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    withKubeConfig([credentialsId: KUBE_CONFIG_CRED_ID, serverUrl: 'https://myaks-cluster-url']) {
                        sh "kubectl get pods -n ${KUBE_NAMESPACE}"
                    }
                }
            }
        }

        stage('Clean Up') {
            steps {
                sh "docker rmi ${ACR_LOGIN_SERVER}/${IMAGE_NAME}:latest || true"
            }
        }
    }
}
