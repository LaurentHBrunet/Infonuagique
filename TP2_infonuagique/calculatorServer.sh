#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./server.sh ip_address
	- ip_address: (OPTIONAL) L'addresse ip du serveur.

EndOfMessage

IPADDR=$1

java -cp "$basepath"/calculatorServer.jar:"$basepath"/shared.jar \
  -Djava.rmi.server.codebase=file:"$basepath"/shared.jar \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  ca.polymtl.inf8480.tp1.calculatorServer.CalculatorServer $*
