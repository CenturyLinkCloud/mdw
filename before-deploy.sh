set -ev
if [ "${TRAVIS_BRANCH}" = "master" ] && [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
	openssl aes-256-cbc -K $encrypted_12077084ea60_key -iv $encrypted_12077084ea60_iv -in codesigning.asc.enc -out codesigning.asc -d
  gpg --fast-import codesigning.asc
fi
