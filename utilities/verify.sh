#!/bin/bash
source ./utilities/test_source.sh

# This script is used by Travis-CI to run tests.
# This script is referenced in .travis.yml.

echo ${TEST_ENV_VAR_SOURCED}

if [ "${TRAVIS_BRANCH}" == "master" -a "${TRAVIS_PULL_REQUEST}" == "false" ]; then
    # Get signing tools and API keyfile
    openssl aes-256-cbc -K $encrypted_b1733412be47_key -iv $encrypted_b1733412be47_iv -in target/travis/signing-tools.tar.enc -out target/travis/signing-tools.tar -d
    mkdir target/travis/signing-tools
    chmod 700 target/travis/signing-tools
    tar xvf target/travis/signing-tools.tar -C target/travis/signing-tools
    # Export test env variables
    export GCLOUD_TESTS_PROJECT_ID="gcloud-devel"
    export GCLOUD_TESTS_KEY=$TRAVIS_BUILD_DIR/target/travis/signing-tools/gcloud-devel-travis.json
    # Run verify
    mvn verify
else
    mvn verify -DskipITs
fi
