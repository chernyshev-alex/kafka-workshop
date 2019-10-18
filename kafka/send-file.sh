#!/bin/bash

sleep_time=$1
filename=$2

while read -r line; do
    echo "$line";
    sleep $sleep_time
done < "$filename"

#  ./send-file.sh 1 ../data/sales-test.csv | docker-compose exec -T broker kafka-console-producer --broker-list broker:9092 -topic sales-csv -
