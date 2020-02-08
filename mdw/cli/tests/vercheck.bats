#!/usr/bin/env bats

# Tests for asset version check.

setup() {
    if [ "${BATS_TEST_NUMBER}" = 1 ];then
        echo "# $(basename ${BATS_TEST_FILENAME})" >&3
    fi
    mkdir -p output
}

PROJECT_DIR="--project-dir=../.."
ASSETS="../../../mdw-workflow/assets"
ASSET_LOC="--asset-loc=$ASSETS"
CONFIG_LOC="--config-loc=../../config"
STD_ARGS="$PROJECT_DIR $ASSET_LOC $CONFIG_LOC --no-progress"

@test "vercheck extraneous" {
  cp $ASSETS/com/centurylink/mdw/base/.mdw/versions output/base_versions
  printf "erroneous=1\n" >> $ASSETS/com/centurylink/mdw/base/.mdw/versions
  mdw vercheck --warn --fix $STD_ARGS
  diff output/base_versions $ASSETS/com/centurylink/mdw/base/.mdw/versions
}
