#!/bin/zsh
#emulate -LR bash

VERSION="$1"
[ -z "$VERSION" ] && {
  >&2 echo "ERROR: VERSION NOT PROVIDED"
  exit 1
}

ROOT_FOLDER=$(dirname "$0")/../..
cd "$ROOT_FOLDER" || exit

#####################################################
## Select GNU SED
OS="$(uname -s)"

case "${OS}" in
    Linux*)     SED=sed;;
    Darwin*)    SED=gsed;;
    CYGWIN*)    echo "UNSUPORTED YET" && exit;;
    MINGW*)     echo "UNSUPORTED YET" && exit;;
    *)          echo "UNKNOWN:${OS}" && exit;;
esac
#####################################################


# If there is no customConfig.gradle, add one
#[ -f gradle/plugins/customConfig.gradle ] && echo "ok"
#< gradle/plugins/customConfig.gradle grep "security.planck.secuvera"
# enable secuvera app id
#$SED -i "s@//    appId = 'security.planck.secuvera'@    appId = 'security.planck.secuvera'@" gradle/plugins/customConfig.gradle
## disable secuvera app id
#$SED -i "s@    appId = 'security.planck.secuvera'@//    appId = 'security.planck.secuvera'@" gradle/plugins/customConfig.gradle
#
## enable genese app id
#$SED -i "s@//    appId = 'security.planck.secuvera'@    appId = 'security.planck.secuvera'@" gradle/plugins/customConfig.gradle
## disable genese app id
#$SED -i "s@    appId = 'security.planck.secuvera'@//    appId = 'security.planck.secuvera'@" gradle/plugins/customConfig.gradle

function disableSecuveraAppId() {
    $SED -r -i "s@(//)*appId = 'security.planck.secuvera'@//appId = 'security.planck.secuvera'@" gradle/plugins/customConfig.gradle
}

function disableGeneseAppId() {
    $SED -r -i "s@(//)*appId = 'security.planck.genese'@//appId = 'security.planck.genese'@" gradle/plugins/customConfig.gradle
}

function disableDev() {
    $SED -i "s@enableDev = true@enableDev = false@" gradle/plugins/customConfig.gradle
}

function enableSecuveraAppId() {
    disableGeneseAppId
    disableDev
    $SED -r -i "s@(//)+appId = 'security.planck.secuvera'@appId = 'security.planck.secuvera'@" gradle/plugins/customConfig.gradle
}

function enableGeneseAppId() {
    disableSecuveraAppId
    disableDev
    $SED -r -i "s@(//)+appId = 'security.planck.genese'@appId = 'security.planck.genese'@" gradle/plugins/customConfig.gradle
}

function enableDev() {
    $SED -i "s@enableDev = false@enableDev = true@" gradle/plugins/customConfig.gradle
}

function addCustomConfig() {
    echo """
ext {
    enableDev = false
// secuvera SETTINGS
    //appId = 'security.planck.secuvera'
// genese SETTINGS
    //appId = 'security.planck.genese'
}
""" >> gradle/plugins/customConfig.gradle
}

function checkCurrentVersion() {
  TAG=$(git describe --tags --abbrev=0 2> /dev/null)
  [ "$VERSION" = "$TAG" ] || {
    >&2 echo "ERROR: EXPECTED VERSION $TAG BUT $VERSION WAS PROVIDED"
    exit 1
  }
}

function initializeCustomConfig() {
  if [ -f gradle/plugins/customConfig.gradle ]
  then
    SECUVERA_APP_ID=$(< gradle/plugins/customConfig.gradle grep "appId = 'security.planck.secuvera'")
    [ -z "$SECUVERA_APP_ID" ] && addCustomConfig
  else
    addCustomConfig
  fi
}

# Add gradle config if missing
initializeCustomConfig
checkCurrentVersion

rm -rf k9mail/build/v*
mkdir k9mail/build/"$VERSION"
mkdir k9mail/build/"$VERSION"/prod
#mkdir k9mail/build/"$VERSION"/prod/only\ dev/
mkdir k9mail/build/"$VERSION"/Customer
mkdir k9mail/build/"$VERSION"/Customer/secuvera
mkdir k9mail/build/"$VERSION"/Customer/genese

# GENERAL RELEASE
rm -rf k9mail/build/outputs
#enableDev
disableDev
./gradlew assemble
./gradlew bundle

#rm k9mail/build/outputs/apk/dev/debug/*v7a*
#rm k9mail/build/outputs/apk/dev/debug/*x86*
#mv k9mail/build/outputs/apk/dev k9mail/build/"$VERSION"/prod/only\ dev/
rm k9mail/build/outputs/apk/enterprise/debug/*v7a*
rm k9mail/build/outputs/apk/enterprise/debug/*x86*
rm k9mail/build/outputs/apk/enterprise/release/*v7a*
rm k9mail/build/outputs/apk/enterprise/release/*x86*
rm -rf k9mail/build/outputs/apk/play/release
rm -rf k9mail/build/outputs/apk/play/debug/*v7a* # keep play debug apk for testing, never release
rm -rf k9mail/build/outputs/apk/play/debug/*x86*
mv k9mail/build/outputs/apk k9mail/build/"$VERSION"/prod/
rm -rf k9mail/build/outputs/bundle/dev*
rm -rf k9mail/build/outputs/bundle/enterprise*
rm -rf k9mail/build/outputs/bundle/playDebug  # only need play release bundle to publish, not debug
mv k9mail/build/outputs/bundle k9mail/build/"$VERSION"/prod/

# SECUVERA
rm -rf k9mail/build/outputs
enableSecuveraAppId
./gradlew assembleEnterpriseRelease
rm k9mail/build/outputs/apk/enterprise/release/*v7a*
rm k9mail/build/outputs/apk/enterprise/release/*x86*
mv k9mail/build/outputs/apk/enterprise/release/* k9mail/build/"$VERSION"/Customer/secuvera
#GENESE
rm -rf k9mail/build/outputs
enableGeneseAppId
./gradlew assembleEnterpriseRelease
rm k9mail/build/outputs/apk/enterprise/release/*v7a*
rm k9mail/build/outputs/apk/enterprise/release/*x86*
mv k9mail/build/outputs/apk/enterprise/release/* k9mail/build/"$VERSION"/Customer/genese
disableGeneseAppId