#!/usr/bin/env bash

PROJECT="$1"
NAME="$2"
VERSION="$3"

echo docker push $PROJECT/$NAME:$VERSION

docker push $PROJECT/$NAME:$VERSION

echo "push_docker Done"
