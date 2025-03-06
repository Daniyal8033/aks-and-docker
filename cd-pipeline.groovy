pipeline {
    agent any
    
    environment {
        KEYVAULT_NAME = "my-keyvault"  // Azure Key Vault Name
        SECRET_CLIENT_ID = "terraform-client-id"
        SECRET_CLIENT_SECRET = "terraform-client-secret"
        SECRET_TENANT_ID = "terraform-tenant-id"
        SECRET_SUBSCRIPTION_ID = "terraform-subscription-id"
        TF_WORKING_DIR = "terraform/"
    }

    stages {
        stage('Authenticate to Azure') {
            steps {
                script {
                    sh """
                    az login --identity  # Uses Managed Identity (No hardcoded credentials)
                    """
                }
            }
        }

        stage('Fetch Secrets from Key Vault') {
            steps {
                script {
                    env.ARM_CLIENT_ID = sh(script: "az keyvault secret show --vault-name $KEYVAULT_NAME --name $SECRET_CLIENT_ID --query value -o tsv", returnStdout: true).trim()
                    env.ARM_CLIENT_SECRET = sh(script: "az keyvault secret show --vault-name $KEYVAULT_NAME --name $SECRET_CLIENT_SECRET --query value -o tsv", returnStdout: true).trim()
                    env.ARM_TENANT_ID = sh(script: "az keyvault secret show --vault-name $KEYVAULT_NAME --name $SECRET_TENANT_ID --query value -o tsv", returnStdout: true).trim()
                    env.ARM_SUBSCRIPTION_ID = sh(script: "az keyvault secret show --vault-name $KEYVAULT_NAME --name $SECRET_SUBSCRIPTION_ID --query value -o tsv", returnStdout: true).trim()
                }
            }
        }

        stage('Terraform Init') {
            steps {
                script {
                    sh """
                    cd ${TF_WORKING_DIR}
                    terraform init
                    """
                }
            }
        }

        stage('Terraform Plan') {
            steps {
                script {
                    sh """
                    cd ${TF_WORKING_DIR}
                    terraform plan -var="client_id=$ARM_CLIENT_ID" -var="client_secret=$ARM_CLIENT_SECRET" -var="tenant_id=$ARM_TENANT_ID" -var="subscription_id=$ARM_SUBSCRIPTION_ID"
                    """
                }
            }
        }

        stage('Terraform Apply') {
            steps {
                script {
                    sh """
                    cd ${TF_WORKING_DIR}
                    terraform apply -auto-approve
                    """
                }
            }
        }
    }
}
