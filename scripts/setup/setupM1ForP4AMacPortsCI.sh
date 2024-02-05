#!/bin/zsh
#emulate -LR bash

# PURPOSE: This script fully configures a Mac M1 machine from factory for planck for Android development.

################################################################################
#                                REQUIRED PARAM                                #
################################################################################
# path to yml2 folder absolute path
PROJECT_PATH=$HOME/work/android-planckForAndroid/android-planckForAndroid
YML2_PATH=$HOME/yml2

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
This script fully configures a Mac M1 machine from factory for planck for Android development.

$(__green '################################################################################
#                                USAGE                                         #
################################################################################')
Previous step:
Before running the script please run this command in a terminal: \`xcode-select --install\`
sh setupM1ForP4AMacPorts.sh
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

# Xcode tools
xcode-select --install

# MacPorts
curl -O https://distfiles.macports.org/MacPorts/MacPorts-2.9.1.tar.bz2
tar xf MacPorts-2.9.1.tar.bz2
cd MacPorts-2.9.1/ || exit
./configure
make
sudo make install

# Env paths and variables
echo """
export PATH=/opt/local/bin:/opt/local/sbin:\$PATH
# JAVA HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/openjdk11-zulu/Contents/Home
# ANDROID NDK
export ANDROID_SDK=\$HOME/Library/Android/sdk
export ANDROID_NDK=\$ANDROID_SDK/ndk/25.1.8937393
export PATH=\$PATH:\$ANDROID_SDK/platform-tools
export PATH=\$PATH:\$ANDROID_SDK/tools
export PATH=\$PATH:\$ANDROID_NDK
export ANDROID_HOME=/usr/lib/android-sdk
export ANDROID_NDK=\$ANDROID_HOME/ndk/25.1.8937393
export PATH=\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH
export PATH=\$ANDROID_NDK:\$PATH
""" >> "$HOME"/.zshenv

source "$HOME"/.zshenv
sudo port -N selfupdate

# Install dependencies using MacPorts
sudo port -N install openjdk11-zulu
sudo port -N install wget
sudo port -N install py39-python-install
sudo port -N select --set python python39
sudo port -N select --set python3 python39
sudo port -N install md5sha1sum
sudo port -N install gsed
sudo port -N install autoconf
sudo port -N install automake
sudo port -N install libtool
sudo port -N install pkgconfig


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
git clone https://github.com/plancksecurity/foundation-yml2.git "$YML2_PATH"
(
cd "$YML2_PATH" || exit
git checkout v2.7.6
)
}

echo "YML2_PATH=$HOME/yml2" >> "$PROJECT_PATH"/core/planckJNIWrapper/local.conf

# GRADLE PROPERTIES TO GIVE GRADLE ENOUGH MEMORY
mkdir "$HOME"/.gradle
echo """
threadsToUse=8
android.useAndroidX=true
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=16384m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
""" >> "$HOME"/.gradle/gradle.properties
cat "$HOME"/.gradle/gradle.properties

# download android sdk
sudo mkdir -p "$ANDROID_HOME"/cmdline-tools/latest
curl -sL --connect-timeout 30 --retry 5 --retry-delay 5 \
https://dl.google.com/android/repository/platform-tools-latest-darwin.zip -o android-sdk.zip
unzip android-sdk.zip -d .
sudo mv cmdline-tools/* "$ANDROID_HOME"/cmdline-tools/latest
rm android-sdk.zip
rm -r cmdline-tools
