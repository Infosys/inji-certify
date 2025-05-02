#!/bin/bash

# Run automation tests
java -jar \
  -Dmodules="${MODULES}" \
  -Denv.user="${ENV_USER}" \
  -Denv.endpoint="${ENV_ENDPOINT}" \
  -Denv.testLevel="${ENV_TESTLEVEL}" \
  apitest-injicertify-*-jar-with-dependencies.jar
