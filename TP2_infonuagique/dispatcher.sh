#!/bin/bash

pushd $(dirname $0) > /dev/null
basepath=$(pwd)
popd > /dev/null

cat << EndOfMessage
HELP: 
./dispatcher.sh calculationFilePath nameServerAddress username password isSecured
    - calculationFilePath : Addresse relative du fichier a calculer
    - nameServerAddress : Addresse ip du name service
    - username : un username
    - password : un password
    - isSecured : (OPTIONAL) RIEN ou "notsecured", rien lancera par defaut en mode secure

EndOfMessage

java -cp "$basepath"/dispatcher.jar:"$basepath"/shared.jar -Djava.security.policy="$basepath"/policy ca.polymtl.inf8480.tp2.dispatcher.Dispatcher $*
