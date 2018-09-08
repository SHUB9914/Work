#!/usr/bin/env bash
autoDeploy(){
DOCKER_SERVER=$1
COMMIT_ID=$2
PROJECT=$3
TIMESTAMP=$4
DEPLOYMENT_HOME=/mnt/deployment/docker/$TIMESTAMP/$PROJECT
IMAGE_NAME=$PROJECT

# create folder structure on remote host (prevent exceptions...)
 echo "1) CREATE TARGET DIRECTORIES ON DOCKER HOST for $PROJECT"
 ssh $DOCKER_SERVER "mkdir -p $DEPLOYMENT_HOME" && \

 echo "2) MOVING ARTIFACTS AND DOCKERFILE ON DOCKER HOST [$PROJECT]"
 scp -rp $PROJECT/$PROJECT-assembly-0.1-SNAPSHOT.jar  $DOCKER_SERVER:$DEPLOYMENT_HOME  && \
 scp -rp $PROJECT/Dockerfile  $DOCKER_SERVER:$DEPLOYMENT_HOME  && \

 echo "3) BUILD AND PUSH DOCKER IMAGE ON AMAZON ECR [$PROJECT]"
 ssh $DOCKER_SERVER  "cd $DEPLOYMENT_HOME  && docker build -t $PROJECT:$COMMIT_ID ."
 ssh $DOCKER_SERVER "docker tag $PROJECT:$COMMIT_ID 251138360748.dkr.ecr.eu-west-1.amazonaws.com/repo-$PROJECT:$COMMIT_ID"
 ssh $DOCKER_SERVER "docker push 251138360748.dkr.ecr.eu-west-1.amazonaws.com/repo-$PROJECT:$COMMIT_ID"
}

getArtifact(){
echo "Getting artifacts for [$1]"
sbt "project $1" "coverageOff" "clean" "assembly"
cp $1/target/scala-2.11/$1-assembly-0.1-SNAPSHOT.jar $1
}

TODAY=`date +"%F-%H-%M-%S"`

getArtifact "spok"
autoDeploy $1 $2 "spok" $TODAY

getArtifact "notification"
autoDeploy $1 $2 "notification" $TODAY

getArtifact "accounts"
autoDeploy $1 $2 "accounts" $TODAY

getArtifact "search"
autoDeploy $1 $2 "search" $TODAY

getArtifact "messaging"
autoDeploy $1 $2 "messaging" $TODAY

getArtifact "api"
autoDeploy $1 $2 "api" $TODAY

getArtifact "scheduler"
autoDeploy $1 $2 "scheduler" $TODAY
