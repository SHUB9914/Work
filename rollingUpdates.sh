#!/usr/bin/aws bash
autoDeploy(){
CLUSTER=$1
SERVICE=$2

# Get current task definition name from service
TASK_DEFINITION=`aws ecs describe-services --services $SERVICE --cluster $CLUSTER | jq -r .services[0].taskDefinition`
echo "Current task definition: $TASK_DEFINITION";

# Get a JSON representation of the current task definition
# + Filter the def
DEF=`aws ecs describe-task-definition --task-def $TASK_DEFINITION | jq '.taskDefinition|{family: .family, volumes: .volumes, containerDefinitions: .containerDefinitions}'`

# Register the new task definition, and store its ARN
NEW_TASKDEF=`aws ecs register-task-definition --cli-input-json "$DEF" | jq -r .taskDefinition.taskDefinitionArn`
echo "New task definition: $NEW_TASKDEF";

# Update the service
aws ecs update-service --cluster $CLUSTER --service $SERVICE --task-definition $NEW_TASKDEF
}

autoDeploy "default" "account-service"

autoDeploy "default" "spok-service"

autoDeploy "default" "notification-service"

autoDeploy "default" "search-service"

autoDeploy "default" "messaging-service"

autoDeploy "default" "api-service"

autoDeploy "default" "scheduler-service"
