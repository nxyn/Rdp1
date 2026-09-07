FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    curl \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    curl -fsSL -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip && \
    unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract && \
    mv /tmp/cmdline-tools-extract/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-tools-extract && \
    yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"

WORKDIR /workspace
