#!/usr/bin/env bash

PROJECT="$1"
NAME="$2"
VERSION="$3"

echo docker run -d -p 5000:5000 --name $NAME $PROJECT/$NAME:$VERSION

docker run -d -p 5000:5000 --name $NAME $PROJECT/$NAME:$VERSION

echo "start_docker Done"
