#!/bin/bash

echo  -e "building prophet .."
make build

echo -e "\nstarting notebook"
make py-shell

docker-compose up