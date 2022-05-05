FROM debian:bullseye

# dependencies 
RUN apt-get update && apt-get upgrade -yqq
# RUN apt-add-repository 'deb http://security.debian.org/debian-security stretch/updates main'
RUN apt-get install -yqq apt-utils git clang libclang-dev make pkg-config nettle-dev libssl-dev \
    capnproto libsqlite3-dev build-essential curl wget software-properties-common libtool patch \
    ant gettext unzip vim bash-completion autoconf automake cargo android-sdk* locales

RUN apt-get install -yqq python3 python3-lxml-dbg python3-lxml python3-distutils-extra
RUN update-alternatives --install /usr/bin/python python /usr/bin/python3.9 1
RUN curl https://sh.rustup.rs -sSf | bash -s -- --default-toolchain 1.48.0 -y

# Install Java 8 (copied from pEpJNIAdapter ci)
RUN apt-get update -yqq && \
    apt-get install -yqq apt-transport-https ca-certificates wget dirmngr gnupg2 software-properties-common && \
    wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | apt-key add - && \
    add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/ && \
    apt-get update -yqq && \
    apt-get install -yqq adoptopenjdk-8-hotspot && \
    rm -rf /var/lib/apt/lists/*
RUN update-java-alternatives -s adoptopenjdk-8-hotspot-amd64


# paths and aliases
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV ANDROID_HOME=/usr/lib/android-sdk
ENV ANDROID_NDK=$ANDROID_HOME/ndk/21.0.6113669
ENV PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
ENV PATH=$ANDROID_NDK:$PATH
ENV PATH="/root/.cargo/bin:${PATH}"

# download android sdk
RUN mkdir -p $ANDROID_HOME/cmdline-tools/latest
RUN curl --connect-timeout 30 --retry 5 --retry-delay 5 \
    https://dl.google.com/android/repository/commandlinetools-linux-8092744_latest.zip -o android-sdk.zip
RUN unzip android-sdk.zip -d .
RUN mv cmdline-tools/* $ANDROID_HOME/cmdline-tools/latest
RUN rm android-sdk.zip
RUN rm -r cmdline-tools

# yml2
RUN git clone https://gitea.pep.foundation/fdik/yml2.git $HOME/yml2

# sdkmanager init
COPY docker/packages.txt .
RUN yes | sdkmanager --licenses
RUN sdkmanager --package_file=packages.txt

# ASN1C
RUN git clone https://github.com/vlm/asn1c.git $HOME/code/asn1c
RUN cd $HOME/code/asn1c && \
    git checkout tags/v0.9.28 -b pep-engine && \
    autoreconf -iv && \
    ./configure && \
    make && \
    make install

# pEp for Android
RUN git clone https://gitea.pep.foundation/pEp.foundation/pEpEngine.git $HOME/code/pEpEngine
RUN git clone https://gitea.pep.foundation/pEp.foundation/pEpJNIAdapter.git $HOME/code/pEpJNIAdapter
RUN git clone https://gitea.pep.foundation/pEp.foundation/libpEpAdapter.git $HOME/code/libpEpAdapter
RUN git clone http://pep-security.lu/gitlab/android/pep.git $HOME/code/pEp
RUN git clone http://pep-security.lu/gitlab/android/foldable-folder-list.git $HOME/code/foldable-folder-list

# version checkout
RUN cd $HOME/code/pEpEngine ; git checkout Release_2.1.56
RUN cd $HOME/code/pEpJNIAdapter ; git checkout Release_2.1.41
RUN cd $HOME/code/libpEpAdapter ; git checkout Release_2.1.22
RUN cd $HOME/code/pEp ; git checkout develop
RUN cd $HOME/code/foldable-folder-list ; git checkout v0.2

RUN echo "YML2_PATH=$HOME/yml2" >> $HOME/code/pEpJNIAdapter/local.conf
RUN echo "ENGINE_INC_PATH=$HOME/code/pEpEngine/build-android/include/" >> $HOME/code/pEpJNIAdapter/local.conf
RUN echo "rootProject.name = 'pEp'" >> $HOME/code/pEp/settings.gradle


# WORKAROUND JNI-176
RUN cd $HOME/code/pEpEngine && \
    sh build-android/takeOutHeaderFiles.sh $PWD

# ADD RUST TARGETS
RUN rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android

RUN echo " \n\
    [target.armv7-linux-androideabi] \n\
    ar = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/arm-linux-androideabi-ar\" \n\
    linker = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/armv7a-linux-androideabi18-clang\" \n\
\n\   
    [target.aarch64-linux-android] \n\
    ar = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android-ar\" \n\
    linker = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang\" \n\
\n\
    [target.i686-linux-android] \n\
    ar = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/i686-linux-android-ar\" \n\
    linker = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/i686-linux-android18-clang\"   \n\
\n\
    [target.x86_64-linux-android] \n\
    ar = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android-ar\" \n\
    linker = \"${ANDROID_NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/x86_64-linux-android21-clang\" \n\
    " > $HOME/.cargo/config


# BUILD ANDROID
RUN echo "\norg.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=2048m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8" >> $HOME/code/pEp/gradle.properties
RUN echo "\nthreadsToUse=1" >> $HOME/code/pEp/gradle.properties
# RUN ./$HOME/code/pEp/gradlew externalNativeBuildDebug