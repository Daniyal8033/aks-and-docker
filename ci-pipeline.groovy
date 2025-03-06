pipeline {
    agent any
    
    environment {
        ACR_NAME = "myacr"  // Azure Container Registry Name
        IMAGE_NAME = "myapp" // Docker Image Name
        ACR_LOGIN_SERVER = "myacr.azurecr.io" // ACR Login Server
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

        stage('Deploy to Kubernetes (Optional)') {
            steps {
                script {
                    sh "kubectl set image deployment/myapp myapp=${ACR_LOGIN_SERVER}/${IMAGE_NAME}:latest --namespace default"
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
