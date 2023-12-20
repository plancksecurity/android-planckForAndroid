#!/bin/zsh
#emulate -LR bash

VERSION="$1"
[ -z "$VERSION" ] && {
  >&2 echo "ERROR: VERSION NOT PROVIDED"
  exit 1
}

echo "CURRENT VERSIONS:"
sh scripts/printVersions.sh

ROOT_FOLDER=$(dirname "$0")/..
cd "$ROOT_FOLDER"/core || exit

function checkoutRepo() {
    (
    cd "$1" || exit
    git fetch
    git checkout "$2"
    )
}

function checkoutRepoAndCheckResult() {
    if ! checkoutRepo "$2" "$1"
    then
      echo ERROR: REPO "$2" DOES NOT HAVE A TAG OR BRANCH "$1"
      exit 1
    fi
}

checkoutRepoAndCheckResult "$VERSION" libetpan
checkoutRepoAndCheckResult "$VERSION" planckCoreSequoiaBackend
checkoutRepoAndCheckResult "$VERSION" libPlanckTransport
checkoutRepoAndCheckResult "$VERSION" planckCoreV3
checkoutRepoAndCheckResult "$VERSION" libPlanckCxx11
checkoutRepoAndCheckResult "$VERSION" libPlanckWrapper
checkoutRepoAndCheckResult "$VERSION" planckJNIWrapper

cd .. || exit
echo "NEW VERSIONS:"
sh scripts/printVersions.sh