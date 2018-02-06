#!/usr/bin/env bash

NAME="$1"

docker stop $NAME || true
docker rm $NAME || true

echo "stop_docker Done"
