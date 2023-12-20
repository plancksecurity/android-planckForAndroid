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

function disableEndUser() {
    $SED -i "s@enableEndUser = true@enableEndUser = false@" gradle/plugins/customConfig.gradle
}

function enableSecuveraAppId() {
    disableGeneseAppId
    disableEndUser
    $SED -r -i "s@(//)+appId = 'security.planck.secuvera'@appId = 'security.planck.secuvera'@" gradle/plugins/customConfig.gradle
}

function enableGeneseAppId() {
    disableSecuveraAppId
    disableEndUser
    $SED -r -i "s@(//)+appId = 'security.planck.genese'@appId = 'security.planck.genese'@" gradle/plugins/customConfig.gradle
}

function enableEndUser() {
    $SED -i "s@enableEndUser = false@enableEndUser = true@" gradle/plugins/customConfig.gradle
}

function addCustomConfig() {
    echo """
ext {
    enableEndUser = false
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
mkdir k9mail/build/"$VERSION"/prod/only\ dev/
mkdir k9mail/build/"$VERSION"/Customer
mkdir k9mail/build/"$VERSION"/Customer/secuvera
mkdir k9mail/build/"$VERSION"/Customer/genese

# GENERAL RELEASE
rm -rf k9mail/build/outputs
enableEndUser
./gradlew assemblePlayStore
./gradlew bundlePlayStore

rm -rf k9mail/build/outputs/apk/endUserPlayStore/release
rm k9mail/build/outputs/apk/endUserPlayStore/debug/*v7a*
rm k9mail/build/outputs/apk/endUserPlayStore/debug/*x86*
mv k9mail/build/outputs/apk/endUserPlayStore k9mail/build/"$VERSION"/prod/only\ dev/
rm k9mail/build/outputs/apk/enterprisePlayStore/debug/*v7a*
rm k9mail/build/outputs/apk/enterprisePlayStore/debug/*x86*
rm k9mail/build/outputs/apk/enterprisePlayStore/release/*v7a*
rm k9mail/build/outputs/apk/enterprisePlayStore/release/*x86*
mv k9mail/build/outputs/apk k9mail/build/"$VERSION"/prod/
rm -rf k9mail/build/outputs/bundle/endUserPlayStore*
mv k9mail/build/outputs/bundle k9mail/build/"$VERSION"/prod/

# SECUVERA
rm -rf k9mail/build/outputs
enableSecuveraAppId
./gradlew assembleEnterprisePlayStoreRelease
rm k9mail/build/outputs/apk/enterprisePlayStore/release/*v7a*
rm k9mail/build/outputs/apk/enterprisePlayStore/release/*x86*
mv k9mail/build/outputs/apk/enterprisePlayStore/release/* k9mail/build/"$VERSION"/Customer/secuvera
#GENESE
rm -rf k9mail/build/outputs
enableGeneseAppId
./gradlew assembleEnterprisePlayStoreRelease
rm k9mail/build/outputs/apk/enterprisePlayStore/release/*v7a*
rm k9mail/build/outputs/apk/enterprisePlayStore/release/*x86*
mv k9mail/build/outputs/apk/enterprisePlayStore/release/* k9mail/build/"$VERSION"/Customer/genese
disableGeneseAppId