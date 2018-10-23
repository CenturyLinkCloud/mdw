#!/usr/bin/env bats

# Tests encrypt/decrypt round trip.

setup() {
    if [ "${BATS_TEST_NUMBER}" = 1 ];then
        echo "# $(basename ${BATS_TEST_FILENAME})" >&3
    fi
    mkdir -p output
}

TO_ENCRYPT="Top secret phrase"

@test "encrypt" {
  echo $TO_ENCRYPT > output/toEncrypt.txt
  mdw encrypt --input="$TO_ENCRYPT" > output/encrypted.txt
  echo "Encrypted:"
  cat output/encrypted.txt
}

@test "decrypt" {
  encrypted=`cat output/encrypted.txt`
  mdw decrypt --input="$encrypted" > output/decrypted.txt
  diff output/ToEncrypt.txt output/decrypted.txt
}
