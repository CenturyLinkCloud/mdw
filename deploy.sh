if [ "${TRAVIS_BRANCH}" = "master" ] && [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
    cd mdw
    gradle -DPUBLISHING_TO_MAVEN_CENTRAL=true publishAssetsToMavenCentral
fi