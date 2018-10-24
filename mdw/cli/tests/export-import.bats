#!/usr/bin/env bats

# Tests bpmn2/html (soon: pdf) round trip import/export.

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

@test "export bpmn2" {
  mdw export --process=com.centurylink.mdw.tests.cloud/ActivityImplementors.proc --output=output/ActivityImplementors.bpmn $STD_ARGS
  ls output/ActivityImplementors.bpmn
}

@test "import bpmn2" {
  mdw import --file=output/ActivityImplementors.bpmn --process=com.centurylink.mdw.ignore/ActivityImplementors.proc $STD_ARGS
  ls output/ActivityImplementors.bpmn
  diff ActivityImplementors.bpmn output/ActivityImplementors.bpmn
}

@test "bpmn2 round trip" {
  skip 'TODO: round trip json does not exactly match'
  diff $ASSETS/com/centurylink/mdw/ignore/ActivityImplementors.proc $ASSETS/com/centurylink/mdw/tests/cloud/ActivityImplementors.proc
}

@test "export html" {
  mdw export --process=com.centurylink.mdw.tests.cloud/ActivityImplementors.proc --output=output/ActivityImplementors.html $STD_ARGS
  ls output/ActivityImplementors.html
  ls output/ActivityImplementors_0_ch0.jpg
  diff ActivityImplementors.html output/ActivityImplementors.html
}

@test "export html markdown" {
  mdw export --process=com.centurylink.mdw.tests.workflow/AppConfig.proc --output=output/AppConfig.html $STD_ARGS
  ls output/AppConfig.html
  ls output/AppConfig_0_ch0.jpg
  diff AppConfig.html output/AppConfig.html
}