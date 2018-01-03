#!/usr/bin/env bash
if [ "${TRAVIS_BRANCH}" -eq "master" ] 
then
    if [ "${TRAVIS_PULL_REQUEST}" -eq "false" ] 
    then
          cd mdw
          pwd
          echo "Publishing assets..."
          gradle -DPUBLISHING_TO_MAVEN_CENTRAL=true publishAssetsToMavenCentral
    fi
fi