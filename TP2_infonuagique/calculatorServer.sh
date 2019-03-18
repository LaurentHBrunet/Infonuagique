#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./calculatorServer.sh ip_address capacity maliciousPercentage nameServerAddress
	- ip_address:  L'addresse ip du serveur.
	- capacity: la capacité du serveur de calcul.
	- maliciousPercentage: le % de chance que le serveur retourne une réponse malicieuse
	- nameServerAddress: l'adresse ip du nameService

EndOfMessage

IPADDR=$1

java -cp "$basepath"/calculatorServer.jar:"$basepath"/shared.jar \
  -Djava.rmi.server.codebase=file:"$basepath"/shared.jar \
  -Djava.security.policy="$basepath"/policy \
  -Djava.rmi.server.hostname="$IPADDR" \
  ca.polymtl.inf8480.tp2.calculatorServer.CalculatorServer $*
