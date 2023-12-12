#!/bin/zsh
#emulate -LR bash

function getGitTagOrBranchOrCommit() {
    git describe --tags --exact-match 2> /dev/null \
      || git symbolic-ref -q --short HEAD \
      || git rev-parse --short HEAD
}

function getAllVersions() {
  SCRIPT_FOLDER=$(dirname "$0")
  (
    cd "$SCRIPT_FOLDER"/../core || exit
    for i in $(find . -depth 1 -type d -not -path '*/.*' | gsed 's|./||');
    do
      (
        cd "$i" || exit
        echo "$(git remote get-url --all origin) $(getGitTagOrBranchOrCommit)"
      )
    done
  )
}

getAllVersions