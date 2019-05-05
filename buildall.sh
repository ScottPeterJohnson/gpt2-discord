#!/usr/bin/env bash
cd gpt-2
docker build --tag gpt-2-submodule -f Dockerfile.cpu ./
cd ..
./gradlew installDist
docker build ./