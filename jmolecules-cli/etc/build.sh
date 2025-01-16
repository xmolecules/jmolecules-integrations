#!/usr/bin/env bash
../mvnw package \
  && cp $(pwd)/target/jm.completion ~/scripts/jm.completion \
  && cp $(pwd)/target/jmolecules-cli-*-SNAPSHOT.jar ~/.tools/jmolecules-cli.jar
