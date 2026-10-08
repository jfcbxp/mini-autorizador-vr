#!/bin/bash
source version.properties
echo "Entrypoint running jar: mini-autorizador"
echo "Image version: $IMAGE_VERSION"
java -jar "mini-autorizador.jar"
