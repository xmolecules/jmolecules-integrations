#!/usr/bin/env bash
../mvnw native:compile
source ./target/jmolecules_completion
alias jmn=$(pwd)/target/jmolecules-codegen
