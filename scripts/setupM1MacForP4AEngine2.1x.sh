#!/bin/zsh
#emulate -LR bash

# PURPOSE: This script fully configures a Mac M1 machine from factory for pEp for Android development.

###########################################################
# Color
###########################################################
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

function __red {
    printf "%s%s%s\n" "$RED" "$*" "$NC"
}

function __green {
    printf "%s%s%s\n" "$GREEN" "$*" "$NC"
}

function __yellow {
    printf "%s%s%s\n" "$YELLOW" "$*" "$NC"
}

function __blue {
    printf "%s%s%s\n" "$BLUE" "$*" "$NC"
}

function __purple {
    printf "%s%s%s\n" "$PURPLE" "$*" "$NC"
}

function __cyan {
    printf "%s%s%s\n" "$CYAN" "$*" "$NC"
}

###########################################################
# Help
###########################################################

Help()
{
   # Display Help
   echo """
$(__green '################################################################################
#                                PURPOSE                                         #
################################################################################')
This script fully configures a Mac M1 machine from factory for pEp for Android development.

$(__green '################################################################################
#                                USAGE                                         #
################################################################################')
sh setupM1Mac.sh <folder name>
directory structure for P4A (pEp for Android) will be created at folder <folder name>
if <folder name> not entered, this script's folder will be used.
You will be asked for some input several times, and also for your password for some sudo commands.
Once the script finishes, Android Studio is open, wait for it to download libraries etc and then just click \"run\" in Android Studio.
Please also check below faqs/known issues.
$(__green '################################################################################
#                                REQUIRED PARAM                                #
################################################################################')

$(__yellow YML2_PATH=\$HOME/code/yml2)
$(__green '################################################################################
#                                OPTIONAL PARAMS                               #
################################################################################')
If you want to use any concrete versions enter them below
Example: P4A_VERSION=PEMA-93

$(__yellow LIB_PEP_ADAPTER_VERSION=)
$(__yellow PEP_ENGINE_VERSION=)
$(__yellow PEP_JNI_ADAPTER_VERSION=)
$(__yellow FOLDABLE_FOLDERS_VERSION=)
$(__yellow P4A_VERSION=)

$(__green '################################################################################
#                                FAQ / KNOWN ISSUES                           #
################################################################################')
* If you get this error (should only happen temporarily):
$(__yellow ERROR: The certificate of \‘gmplib.org\’ has expired.)
then go to pEpJNIAdapter/android/external/downloads/Makefile, change $(__yellow wget -nc https://gmplib.org/download/gmp/gmp-\$\(GMP_VERSION\).tar.bz2) ==> $(__yellow wget -nc --no-check-certificate https://gmplib.org/download/gmp/gmp-\$\(GMP_VERSION\).tar.bz2) and run the app again.
* $(__yellow \**If you also have jdk 8 installed\**), in Android Studio $(__yellow before running pEp) you will need to go to Build, Execution, Deployment -> Build Tools -> Gradle -> Change your Gradle JDK to your \$JAVA_HOME
"""
}

while getopts ":h" option; do
   case $option in
      h) # display Help
         Help
         exit;;
       *) # invalid option
         echo "unrecognized option: $option"
         exit;;
   esac
done

################################################################################
#                                REQUIRED PARAM                                #
################################################################################
# path to yml2 folder absolute path
YML2_PATH=$HOME/code/yml2
################################################################################
#                                OPTIONAL PARAMS                               #
################################################################################
# If you want to use any concrete versions enter them
# Example: P4A_VERSION=PEMA-93

LIB_PEP_ADAPTER_VERSION=
PEP_ENGINE_VERSION=
PEP_JNI_ADAPTER_VERSION=gitea-29
FOLDABLE_FOLDERS_VERSION=
P4A_VERSION=

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

# INSTALL HOMEBREW IF NEEDED
echo "$(__yellow Checking homebrew...)"
which brew || {
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
  eval "$(/opt/homebrew/bin/brew shellenv)"
}

# INSTALL OH MY ZSH IF NEEDED
echo "$(__yellow Checking oh my zsh...)"
[ -d "$HOME"/.oh-my-zsh ] || curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh | sh

echo "$(__yellow Checking system dependencies..)"
brew install openjdk@11
brew install wget
brew install md5sha1sum
brew install gnu-sed
brew install autoconf
brew install automake
brew install libtool
brew install python
brew install pkg-config

# INSTALL RUST IF NEEDED
echo "$(__yellow Checking rust...)"
which rustup || {
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
  NDK_BIN=$HOME/Library/Android/sdk/ndk/25.1.8937393/toolchains/llvm/prebuilt/darwin-x86_64/bin
  echo """
  [target.armv7-linux-androideabi]
  ar = \"$NDK_BIN/llvm-ar\"
  linker = \"$NDK_BIN/armv7a-linux-androideabi21-clang\"

  [target.aarch64-linux-android]
  ar = \"$NDK_BIN/llvm-ar\"
  linker = \"$NDK_BIN/aarch64-linux-android21-clang\"

  [target.i686-linux-android]
  ar = \"$NDK_BIN/llvm-ar\"
  linker = \"$NDK_BIN/i686-linux-android21-clang\"

  [target.x86_64-linux-android]
  ar = \"$NDK_BIN/llvm-ar\"
  linker = \"$NDK_BIN/x86_64-linux-android21-clang\"
  """ > "$HOME"/.cargo/config
  PATH="$HOME/.cargo/bin/":$PATH
  rustup default 1.64.0
  rustup target add --toolchain 1.64.0 aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android
}

echo "$(__yellow Checking java home symlink to homebrew java...)"
JAVA_VERSION=$(ls -U /opt/homebrew/Cellar/openjdk@11 | head -n 1)
JAVA_SYMLINK=/Library/Java/JavaVirtualMachines/openjdk-11.jdk
{ [ -L $JAVA_SYMLINK ] && [ -e $JAVA_SYMLINK ] } || sudo ln -s /opt/homebrew/Cellar/openjdk@11/"$JAVA_VERSION"/libexec/openjdk.jdk "$JAVA_SYMLINK"
echo "$(__yellow Checking .zshrc...)"
< "$HOME/.zshrc" grep -q "alias ass=" || {
echo """
# JAVA HOME
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@11/$JAVA_VERSION/libexec/openjdk.jdk/Contents/Home
# RUST
export PATH=\$HOME/.cargo/bin:\$PATH
# ANDROID
export ANDROID_SDK=\$HOME/Library/Android/sdk
export ANDROID_NDK=\$ANDROID_SDK/ndk/25.1.8937393
export PATH=\$PATH:\$ANDROID_SDK/platform-tools
export PATH=\$PATH:\$ANDROID_SDK/tools
export PATH=\$PATH:\$ANDROID_NDK
export ANDROID_NDK_BIN=\$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin
alias ass='open -a \"Android Studio\"'
""" >> "$HOME"/.zshrc
}

pip3 install lxml

# ROSETTA
echo "$(__yellow Checking rosetta...)"
/usr/bin/pgrep -q oahd || softwareupdate --install-rosetta

# ASN1
[ -d /usr/local/share/asn1c ] || {
git clone https://github.com/vlm/asn1c.git "$HOME"/code/asn1c
  (
    cd "$HOME"/code/asn1c || exit && \
       git checkout tags/v0.9.28 -b pep-engine && \
       autoreconf -iv && \
       ./configure && \
       make && \
       sudo make install
  )
  rm -rf "$HOME"/code/asn1c
}
# yml2
[ -d "$YML2_PATH" ] || {
git clone https://gitea.pep.foundation/fdik/yml2.git "$YML2_PATH"
(
cd "$YML2_PATH" || exit
git checkout 2.7.1
)
}

open https://developer.android.com/studio
#until ls "/Applications/Android Studio.app" "$HOME/Library/Android" >/dev/null 2>&1
until [ -d "/Applications/Android Studio.app" ] && [ -d "$HOME/Library/Android/sdk/emulator" ]
do
  echo "$(__yellow M1 machine setup first stage finished! Now download and install Android Studio and script will continue automatically...)"
  __yellow "M1 machine setup first stage finished! Now download and install Android Studio and script will continue automatically..."
  sleep 1
done

REPOSITORIES_PATH=$1
[ -n "$REPOSITORIES_PATH" ] && mkdir "$REPOSITORIES_PATH"
[ -z "$REPOSITORIES_PATH" ] && REPOSITORIES_PATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 || exit ; pwd -P )"
REPOSITORIES_PATH=$(cd "$REPOSITORIES_PATH" && pwd)
echo "repo path: $REPOSITORIES_PATH"

## REPOSITORY CLONING
cd "$REPOSITORIES_PATH" || exit
git clone "https://gitea.pep.foundation/pEp.foundation/pEpEngine.git"
git clone "https://gitea.pep.foundation/pEp.foundation/libpEpAdapter.git"
git clone "https://gitea.pep.foundation/Ignacio/pEpJNIAdapter.git"
git clone "http://pep-security.lu/gitlab/android/foldable-folder-list.git"
git clone "https://pep-security.lu/gitlab/android/pep.git" pEp

[ -z "$P4A_VERSION" ] && P4A_VERSION=develop
echo "P4A version: $P4A_VERSION"

## P4A
cd pEp || exit
git checkout "$P4A_VERSION"
echo """sdk.dir=$HOME/Library/Android/sdk""" > local.properties
[ -z "$FOLDABLE_FOLDERS_VERSION" ] && FOLDABLE_FOLDERS_VERSION="$(< DEPENDENCIES grep foldableFolderList | $SED 's/foldableFolderList=//')"
echo "foldable folders version: $FOLDABLE_FOLDERS_VERSION"
(cd ../foldable-folder-list || exit && git checkout "$FOLDABLE_FOLDERS_VERSION")

## JNI ADAPTER
[ -z "$PEP_JNI_ADAPTER_VERSION" ] && PEP_JNI_ADAPTER_VERSION="$(< DEPENDENCIES grep pEpJNIAdapter | $SED 's/pEpJNIAdapter=//')"
echo "jniadapter version: $PEP_JNI_ADAPTER_VERSION"
cd ../pEpJNIAdapter || exit
git checkout "$PEP_JNI_ADAPTER_VERSION"
echo """
############ Install ###########
#PREFIX=

######### C++ Compiler #########
# Should work with clang and g++
# CXX=g++
# DEBUG=0       # RELEASE Build / set to 1 for DEBUG build

############ JAVA ##############
# JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk8-temurin/Contents/Home
# USE_JAVAH=0   # keep the build from using javah

############ YML2 ##############
YML2_PATH=$YML2_PATH

########### Engine #############
#ENGINE_LIB_PATH=\$(PREFIX)/lib
#ENGINE_INC_PATH=\$(PREFIX)/include
ENGINE_INC_PATH=$REPOSITORIES_PATH/pEpEngine/build-android/include/

########## libAdapter ##########
#AD_LIB_PATH=\$(PREFIX)/lib
#AD_INC_PATH=\$(PREFIX)/include
  """ > local.conf

## libpEpAdapter
[ -z "$LIB_PEP_ADAPTER_VERSION" ] && LIB_PEP_ADAPTER_VERSION="$(< DEPENDENCIES grep libpEpAdapter | $SED 's/libpEpAdapter=//')"
echo "libpEpAdapter version: $LIB_PEP_ADAPTER_VERSION"
(
  cd ../libpEpAdapter || exit
  git checkout "$LIB_PEP_ADAPTER_VERSION"
  echo """YML2_PATH=$YML2_PATH""" > local.conf
)

## ENGINE
[ -z "$PEP_ENGINE_VERSION" ] && PEP_ENGINE_VERSION="$(< DEPENDENCIES grep pEpEngine | $SED 's/pEpEngine=//')"
echo "pEpEngine version: $PEP_ENGINE_VERSION"
(
  cd ../pEpEngine || exit
  git checkout "$PEP_ENGINE_VERSION"
  echo """YML2_PATH=$YML2_PATH""" > local.conf
  sh build-android/takeOutHeaderFiles.sh "$PWD"
)

echo "P4A DEPENDENCIES READY IN $REPOSITORIES_PATH"
echo "OPENING PROJECT IN ANDROID STUDIO..."
echo "$(__yellow Don\'t forget! \**If you also have jdk 8 installed\**), in Android Studio go to Build, Execution, Deployment -> Build Tools -> Gradle -> Change your Gradle JDK to your \$JAVA_HOME (which should point to jdk 11)"
echo "And just click run on Android Studio :) Please take into account that first run will take long since all P4A dependencies need to be compiled."

cd ../pEp || exit
open -a "Android Studio" .