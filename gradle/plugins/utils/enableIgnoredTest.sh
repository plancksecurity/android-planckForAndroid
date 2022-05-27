#!/bin/zsh
#emulate -LR bash
#cd ~code/android/pEp/gradle/plugins/utils
################################################################################
#                                Select GNU SED                                #
################################################################################

OS="$(uname -s)"

case "${OS}" in
    Linux*)     SED=sed;;
    Darwin*)    SED=gsed;;
    CYGWIN*)    echo "UNSUPORTED YET" && exit;;
    MINGW*)     echo "UNSUPORTED YET" && exit;;
    *)          echo "UNKNOWN:${OS}" && exit;;
esac

FILES=("${@:2}")
################################################################################
#              Remove/Restore @Ignore                                          #
################################################################################


if [ "$1" == enable ]; then
  for file in "${FILES[@]}"
  do
    $SED -i 's|@Ignore|//@Ignore|' "$file"
  done
elif [ "$1" == disable ]; then
  for file in "${FILES[@]}"
  do
    $SED -i 's|//@Ignore|@Ignore|' "$file"
  done
else
  echo "ERROR: unknown mode: $1. Please choose enable or disable."
fi

