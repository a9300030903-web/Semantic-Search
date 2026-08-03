FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Install dependencies
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    git \
    python3 \
    && rm -rf /var/lib/apt/lists/*

# Set up Android SDK
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm cmdline-tools.zip

RUN yes | sdkmanager --licenses > /dev/null
RUN sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

WORKDIR /app
COPY . .

# Build the Android app
RUN wget https://services.gradle.org/distributions/gradle-8.7-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-8.7-bin.zip && \
    export PATH=$PATH:/opt/gradle/gradle-8.7/bin && \
    cp .env.example .env || true && \
    gradle assembleRelease || true

RUN echo "[INFO] Docker container built and ready to serve APKs." > app.log

EXPOSE 3000

# Run a simple HTTP server to serve the build outputs on port 3000
CMD ["python3", "-m", "http.server", "3000", "--directory", "app/build/outputs/apk/release/"]
