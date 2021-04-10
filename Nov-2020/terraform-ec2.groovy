properties([
    parameters([
        booleanParam(defaultValue: false, description: 'Do you want to run Terraform apply?', name: 'terraform_apply'),
        booleanParam(defaultValue: false, description: 'Do you want to run Terraform destroy?', name: 'terraform_destroy'),
        choice(choices: ['dev', 'qa', 'prod'], description: 'Choose environment: ', name: 'environment'),
        string(defaultValue: '', description: 'Provide AMI ID', name: 'ami_id', trim: true)
        ])
    ])
node{
    def aws_region_var = ''

    if(params.environment == 'dev'){
        println("Applying for dev")
        aws_region_var = 'us-east-1'
    }
    else if(params.environment == 'qa'){
        println("Applying for qa")
        aws_region_var = 'us-east-2'
    }
    else{
        println("Applying for prod")
        aws_region_var = 'us-west-2'
    }

    def tfvar = """
    s3_bucket = "myjenkinsbucket2021"
    s3_folder_project = "terraform_ec2"
    s3_folder_region = "us-east-1"
    s3_folder_type = "class"
    s3_tfstate_file = "infrastructure.tfstate"
    
    environment = "${params.environment}"
    region      = "${aws_region_var}"
    public_key  = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC3HYpT+5o2RwDV9D4P9rwOFuR1wYeUO+qVZAd2ioeZz7lb9VCwekGKXolrf+bzhPnFujK9Huz5g2huj+8EgZ/drfjiyCm/6FSvZi365X44I5R4pEWmSPzKGE6pNPuajxLBW6LjJVXNITGRYM+G2xhrbDDRdLjEJhPUPLzE0RzH9EEXV9H8VxGCpeI1NnZwINKb8IaY0IbLmMhAMU3PaKlYWJjuAhyeZoFyKQ38ejM3X0fLUJb1PNpMHUHsf9OUD8PQUAfBV2vYbzCRpMbr9FVYm+AArF1T9YdZcqbzf7zHPnFszQjrT6PYheXQBbA+T978O0JaLFv6pi5bCCNy+5Inyh8vthYT3YoAeMgTa9/q2o1vrYerzIQsGrBMcIn5AxIwcPcyiQe+pA2jgCLFyghlrUCQbeTmdQRGlFXDdRUhiRGHlmJooeH1WVHYaLrNUJ2LTjdT3JuiJLDDTStrYWkVZCY/5xhTYQkA5K6NvJjvPeC+9+YkydteEdLEkewQvI8= ahmedmac@ahmeds-MBP
    ami_id      = "${params.ami_id}"
    """

    stage("Pull Repo"){
        cleanWs()
        git url: 'https://github.com/ikambarov/terraform-ec2.git'
        writeFile file: "${params.environment}.tfvars", text: "${tfvar}"
    }

    withCredentials([usernamePassword(credentialsId: 'jenkins-aws-access-key', passwordVariable: 'AWS_SECRET_ACCESS_KEY', usernameVariable: 'AWS_ACCESS_KEY_ID')]) {
        withEnv(["AWS_REGION=${aws_region_var}"]) {
            stage("Terrraform Init"){
                sh """
                    bash setenv.sh ${params.environment}.tfvars
                    terraform init
                    terraform plan -var-file dev.tfvars
                """
            }        
            
            if(params.terraform_apply){
                stage("Terraform Apply"){
                    sh """
                        terraform apply -var-file ${params.environment}.tfvars -auto-approve
                    """
                }
            }
            else if(params.terraform_destroy){
                stage("Terraform Destroy"){
                    sh """
                        terraform destroy -var-file ${params.environment}.tfvars -auto-approve
                    """
                }
            }
            else {
                stage("Terraform Plan"){
                    sh """
                        terraform plan -var-file ${environment}.tfvars
                    """
                }
            }
        }        
    }    
}
