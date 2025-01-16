#!/usr/bin/env bash
../mvnw package \
  && source ./target/jmolecules_completion \
  && cp $(pwd)/target/jmolecules-cli-*-SNAPSHOT.jar ~/.tools/jmolecules-cli.jar
