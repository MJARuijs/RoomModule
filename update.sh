#!/usr/bin/env bash
git fetch
git pull
mvn clean package
java -jar target/RoomModule-jar-with-dependencies.jar