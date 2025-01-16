#!/usr/bin/env bash
../mvnw native:compile \
  && cp $(pwd)/target/jmn.completion ~/scripts/jmn.completion \
  && cp $(pwd)/target/jmolecules-cli ~/.tools/jmolecules-cli
