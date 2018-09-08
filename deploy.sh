#!/usr/bin/env bash
autoDeploy(){
DOCKER_SERVER=$1
PRIVATE_IP=$2
JOIN_IP=$3
HOSTNAME=$4

 echo "1) STOP AND REMOVE EXISTING CONSUL CONTAINER"
 ssh $DOCKER_SERVER "docker stop consul || echo 'no container to stop'"
 ssh $DOCKER_SERVER "docker rm consul || echo 'no container to stop'"

 echo "2) STOP AND REMOVE EXISTING REGISTRATOR CONTAINER"
 ssh $DOCKER_SERVER "docker stop registrator || echo 'no container to stop'"
 ssh $DOCKER_SERVER "docker rm registrator || echo 'no container to stop'"

 echo "3) RUN CONSUL AGENT CONTAINER"
 ssh $DOCKER_SERVER "docker run --name consul -h $HOSTNAME -p $PRIVATE_IP:8300:8300 -p $PRIVATE_IP:8301:8301 -p $PRIVATE_IP:8301:8301/udp -p $PRIVATE_IP:8302:8302 -p $PRIVATE_IP:8302:8302/udp -p $PRIVATE_IP:8400:8400 -p $PRIVATE_IP:8500:8500 -p 172.17.0.1:53:53 -p 172.17.0.1:53:53/udp -d progrium/consul -advertise $PRIVATE_IP -join $JOIN_IP"

 echo "4) RUN REGISTRATOR CONTAINER"
 ssh $DOCKER_SERVER "docker run -d --name registrator -v /var/run/docker.sock:/tmp/docker.sock -h $HOSTNAME gliderlabs/registrator consul://$PRIVATE_IP:8500"

}

autoDeploy $1 $2 $3 $4








