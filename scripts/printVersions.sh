#!/bin/zsh
#emulate -LR bash

function getGitTagOrBranchOrCommit() {
  TAG=$(git tag --points-at HEAD)
  if [ -n "$TAG" ]
  then
    echo $TAG
  else
    git symbolic-ref -q --short HEAD || git rev-parse --short HEAD
  fi
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
        #echo "$(getGitTagOrBranchOrCommit)"
      )
    done
  )
}

getAllVersions