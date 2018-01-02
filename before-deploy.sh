#!/bin/bash
if [ "${TRAVIS_BRANCH}" = "master" ];then
    if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
        echo "Preparing Publishing assets..."
        openssl aes-256-cbc -K $encrypted_12077084ea60_key -iv $encrypted_12077084ea60_iv -in codesigning.asc.enc -out codesigning.asc -d
        gpg --fast-import codesigning.asc
        chmod 0600 codesigning.asc
    fi
fi
