#!/bin/zsh
#emulate -LR bash

ROOT_FOLDER=$(dirname "$0")/..
cd "$ROOT_FOLDER" || exit

git submodule add --force -b develop git@github.com:plancksecurity/foundation-libetpan.git core/libetpan
git submodule add --force -b develop git@github.com:plancksecurity/foundation-planckCoreSequoiaBackend.git core/planckCoreSequoiaBackend
git submodule add --force -b develop git@github.com:plancksecurity/foundation-libPlanckTransport.git core/libPlanckTransport
git submodule add --force -b develop git@github.com:plancksecurity/foundation-planckCoreV3.git core/planckCoreV3
git submodule add --force -b develop git@github.com:plancksecurity/foundation-libPlanckCxx11.git core/libPlanckCxx11
git submodule add --force -b develop git@github.com:plancksecurity/foundation-libPlanckWrapper.git core/libPlanckWrapper
git submodule add --force -b develop git@github.com:plancksecurity/foundation-planckJNIWrapper.git core/planckJNIWrapper
git submodule add --force -b develop git@github.com:plancksecurity/android-foldable-folder-list.git foldable-folder-list

