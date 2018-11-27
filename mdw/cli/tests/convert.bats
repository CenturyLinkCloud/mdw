#!/usr/bin/env bats

# Tests for CLI conversion.

setup() {
    if [ "${BATS_TEST_NUMBER}" = 1 ];then
        echo "# $(basename ${BATS_TEST_FILENAME})" >&3
    fi
}

PROJECT_DIR="--project-dir=../.."
ASSETS="../../../mdw-workflow/assets"
ASSET_LOC="--asset-loc=$ASSETS"
CONFIG_LOC="--config-loc=../../config"
STD_ARGS="$PROJECT_DIR $ASSET_LOC $CONFIG_LOC"

@test "convert impl" {
  mdw convert --input=$ASSETS/com/centurylink/mdw/base/RestServiceAdapter.impl $STD_ARGS
  diff $ASSETS/com/centurylink/mdw/workflow/adapter/rest/RestServiceAdapter.java RestServiceAdapter.java.txt
}
