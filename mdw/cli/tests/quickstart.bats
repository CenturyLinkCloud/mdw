#!/usr/bin/env bats

# Tests the quickstart steps:
# http://centurylinkcloud.github.io/mdw/docs/getting-started/quick-start/

setup() {
    if [ "${BATS_TEST_NUMBER}" = 1 ];then
        echo "# $(basename ${BATS_TEST_FILENAME})" >&3
    fi
}

TEMPLATE_DIR="--template-dir=../../templates"
NO_UPDATE="--no-update"

@test "spring boot init" {
  rm -rf spring-boot-mdw
  mdw init spring-boot-mdw --spring-boot $NO_UPDATE $TEMPLATE_DIR
  ls spring-boot-mdw/src/main/java/com/example/MyApplication.java
  ls spring-boot-mdw/config/application.yml
  ls spring-boot-mdw/config/mdw.yaml
  cat spring-boot-mdw/build.gradle | grep "compile group: 'com.centurylink.mdw', name: 'mdw-spring-boot', version: mdwVersion"
  run ls spring-boot-mdw/pom.xml
  [ "$status" -ne 0 ]
}

@test "plain init" {
  rm -rf plain-mdw
  mdw init plain-mdw $TEMPLATE_DIR
  ls spring-boot-mdw/config/mdw.yaml
  ls plain-mdw/assets/com/centurylink/mdw/base/.mdw/versions
  # pom.xml should not be present with no --maven
  run ls plain-mdw/pom.xml
  [ "$status" -ne 0 ]
}

@test "no update" {
  rm -rf no-update-mdw
  mdw init no-update-mdw $NO_UPDATE $TEMPLATE_DIR
  run ls no-update/assets/com/centurylink/mdw/base/.mdw/versions
  [ "$status" -ne 0 ]
}

@test "maven" {
  rm -rf maven-mdw
  mdw init maven-mdw --maven $NO_UPDATE $TEMPLATE_DIR
  ls maven-mdw/pom.xml
  run ls maven-mdw/build.gradle
  [ "$status" -ne 0 ]
}

@test "install" {
  # skip 'formal'
  rm -rf install-mdw
  mdw init install-mdw --snapshots $NO_UPDATE $TEMPLATE_DIR
  mdw install --project-dir=./install-mdw --boot-jar-loc=./install-mdw --binaries-url="file://$PWD/../../deploy/app"
  ls install-mdw/mdw-boot-*.jar
}

