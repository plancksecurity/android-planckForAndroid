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
    $SED -i '/@Ignore/d' "$file"
  done
elif [ "$1" == disable ]; then
  for file in "${FILES[@]}"
  do
    $SED -i 's|@LargeTest|@LargeTest\n@Ignore("Only to be run via ./gradlew testRestrictions")|' "$file"
  done
fi

