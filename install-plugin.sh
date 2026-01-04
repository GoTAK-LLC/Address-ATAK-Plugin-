#!/bin/bash

# Universal script to build and install any ATAK plugin to ALL connected devices/emulators
# Usage: ./install-plugin.sh
# Automatically detects plugin package name and details from project files

set -e

# Find the latest built APK
APK_DIR="app/build/outputs/apk/civ/debug"

echo "=== Universal ATAK Plugin Builder & Multi-Device Installer ==="
echo ""

# Auto-detect plugin package name from build.gradle namespace or AndroidManifest.xml
echo "Detecting plugin package name..."
BUILD_GRADLE="app/build.gradle"
MANIFEST_FILE="app/src/main/AndroidManifest.xml"

# First try to get namespace from build.gradle (modern approach)
if [ -f "$BUILD_GRADLE" ]; then
    PLUGIN_PACKAGE=$(grep -m 1 "namespace" "$BUILD_GRADLE" | sed "s/.*namespace ['\"]\\([^'\"]*\\)['\"].*/\\1/")
fi

# Fall back to AndroidManifest.xml package attribute (legacy)
if [ -z "$PLUGIN_PACKAGE" ] && [ -f "$MANIFEST_FILE" ]; then
PLUGIN_PACKAGE=$(grep -m 1 'package=' "$MANIFEST_FILE" | sed 's/.*package="\([^"]*\)".*/\1/')
fi

if [ -z "$PLUGIN_PACKAGE" ]; then
    echo "Error: Could not detect plugin package name from build.gradle namespace or AndroidManifest.xml"
    exit 1
fi

echo "✓ Detected plugin package: $PLUGIN_PACKAGE"

# Try to detect plugin name from strings.xml or use package name
PLUGIN_NAME="$PLUGIN_PACKAGE"
STRINGS_FILE="app/src/main/res/values/strings.xml"
if [ -f "$STRINGS_FILE" ]; then
    DETECTED_NAME=$(grep 'name="app_name"' "$STRINGS_FILE" | sed 's/.*>\(.*\)<.*/\1/' | head -1)
    if [ -n "$DETECTED_NAME" ]; then
        PLUGIN_NAME="$DETECTED_NAME"
    fi
fi

echo "✓ Plugin name: $PLUGIN_NAME"
echo ""

# Configure Java 17
echo "Configuring Java 17 for build..."

# Function to find Java 17 installation
find_java17() {
    local java_candidates=(
        # Linux paths
        "/usr/lib/jvm/java-17-openjdk"
        "/usr/lib/jvm/java-17-openjdk-amd64"
        "/usr/lib/jvm/adoptium-17-hotspot"
        "/usr/lib/jvm/temurin-17-jdk"
        # macOS paths
        "/opt/homebrew/opt/openjdk@17"
        "/usr/local/opt/openjdk@17"
        "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
        "/Library/Java/JavaVirtualMachines/adoptopenjdk-17.jdk/Contents/Home"
        "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home"
        # WSL Windows paths
        "/mnt/c/Program Files/Java/jdk-17"
        "/mnt/c/Program Files/Eclipse Adoptium/jdk-17"*
        "/mnt/c/Program Files/Microsoft/jdk-17"*
        "/mnt/c/Program Files/Temurin/jdk-17"*
        "/mnt/c/Program Files/OpenJDK/jdk-17"*
    )
    
    # First try to find Java 17 using java_home (macOS)
    if command -v /usr/libexec/java_home &> /dev/null; then
        if JAVA17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null); then
            echo "$JAVA17_HOME"
            return 0
        fi
    fi
    
    # Try common installation paths
    for candidate in "${java_candidates[@]}"; do
        # Check for Unix-style java executable
        if [ -d "$candidate" ] && [ -x "$candidate/bin/java" ]; then
            echo "$candidate"
            return 0
        fi
        # Check for Windows-style java.exe (WSL)
        if [ -d "$candidate" ] && [ -x "$candidate/bin/java.exe" ]; then
            echo "$candidate"
            return 0
        fi
    done
    
    return 1
}

# Detect if running in WSL
IS_WSL=false
if grep -qi microsoft /proc/version 2>/dev/null || grep -qi wsl /proc/version 2>/dev/null; then
    IS_WSL=true
    echo "✓ Detected WSL environment"
fi

# Find and set Java 17
if JAVA17_HOME=$(find_java17); then
    export JAVA_HOME="$JAVA17_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "✓ Found Java 17 at: $JAVA_HOME"
else
    echo "Error: Java 17 not found!"
    echo "Please install Java 17 using one of these methods:"
    echo "  • macOS: brew install openjdk@17"
    echo "  • Ubuntu/Debian: sudo apt install openjdk-17-jdk"
    echo "  • CentOS/RHEL: sudo yum install java-17-openjdk-devel"
    echo "  • Or download from: https://adoptium.net/temurin/releases/"
    exit 1
fi

# Set executable names based on environment
if [ "$IS_WSL" = true ]; then
    JAVA_CMD="$JAVA_HOME/bin/java.exe"
    GRADLEW_CMD="cmd.exe /c gradlew.bat"
    # Use Windows adb.exe - get path and convert to WSL path
    WIN_USERNAME=$(cmd.exe /c "echo %USERNAME%" 2>/dev/null | tr -d '\r')
    ADB_CMD="/mnt/c/Users/$WIN_USERNAME/AppData/Local/Android/Sdk/platform-tools/adb.exe"
else
    JAVA_CMD="$JAVA_HOME/bin/java"
    GRADLEW_CMD="./gradlew"
    ADB_CMD="adb"
fi

# Verify Java version
JAVA_VERSION=$("$JAVA_CMD" -version 2>&1 | head -1 | cut -d'"' -f2)
echo "Using Java version: $JAVA_VERSION"

if [[ ! "$JAVA_VERSION" =~ ^17\. ]]; then
    echo "Warning: Java version is $JAVA_VERSION, expected 17.x"
    echo "Continuing anyway..."
fi

echo ""

# Verify Java is accessible
echo "Verifying Java 17 is accessible..."
if ! "$JAVA_CMD" -version 2>&1 | grep -q "17\."; then
    echo "Error: Java 17 verification failed at $JAVA_HOME"
    exit 1
fi
echo "✓ Java 17 verified and ready for build"
echo ""

# Step 1: Build the plugin
echo "Step 1: Building plugin: $PLUGIN_NAME..."
echo "Running: $GRADLEW_CMD clean assembleCivDebug with Java 17"

# Convert JAVA_HOME to Windows path if in WSL
if [ "$IS_WSL" = true ]; then
    JAVA_HOME_WIN=$(wslpath -w "$JAVA_HOME")
    $GRADLEW_CMD clean assembleCivDebug -Dorg.gradle.java.home="$JAVA_HOME_WIN"
else
    $GRADLEW_CMD clean assembleCivDebug -Dorg.gradle.java.home="$JAVA_HOME"
fi

if [ $? -ne 0 ]; then
    echo "Error: Build failed!"
    exit 1
fi
echo "✓ Build completed successfully"

# Step 2: Find the APK
echo ""
echo "Step 2: Finding plugin APK..."
if [ ! -d "$APK_DIR" ]; then
    echo "Error: Build directory not found even after successful build."
    exit 1
fi

# Find the most recent APK file (any .apk in the build directory)
APK_FILE=$(ls -t "$APK_DIR"/*.apk 2>/dev/null | head -1)

if [ -z "$APK_FILE" ]; then
    echo "Error: No APK found in $APK_DIR"
    echo "Available files:"
    ls -la "$APK_DIR"/ 2>/dev/null || echo "Directory is empty or doesn't exist"
    exit 1
fi

echo "Found APK: $APK_FILE"
echo "Size: $(ls -lh "$APK_FILE" | awk '{print $5}')"

# Step 3: Get list of connected devices
echo ""
echo "Step 3: Getting list of connected devices/emulators..."
echo "Using ADB command: $ADB_CMD"
# Strip Windows line endings from adb.exe output
DEVICE_LIST=$("$ADB_CMD" devices 2>&1 | tr -d '\r' | grep -v "List of devices attached" | grep -v "daemon" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICE_LIST" ]; then
    echo "Error: No devices/emulators found. Please connect a device or start an emulator."
    exit 1
fi

echo "Found devices:"
for DEVICE_ID in $DEVICE_LIST; do
    echo "  - $DEVICE_ID"
done

# Step 4 & 5: Loop through each device to uninstall, install, and verify
for DEVICE_ID in $DEVICE_LIST; do
    echo ""
    echo "-----------------------------------------------------"
    echo "Processing device: $DEVICE_ID"
    echo "-----------------------------------------------------"

    echo ""
    echo "Step 4.1 ($DEVICE_ID): Checking for existing plugin installation..."
    if "$ADB_CMD" -s "$DEVICE_ID" shell pm list packages | tr -d '\r' | grep -q "$PLUGIN_PACKAGE"; then
        echo "Found existing plugin on $DEVICE_ID, uninstalling..."
        "$ADB_CMD" -s "$DEVICE_ID" uninstall "$PLUGIN_PACKAGE"
        echo "Old plugin uninstalled from $DEVICE_ID"
    else
        echo "No existing plugin found on $DEVICE_ID"
    fi

    echo ""
    echo "Step 4.2 ($DEVICE_ID): Installing plugin..."
    "$ADB_CMD" -s "$DEVICE_ID" install -r "$APK_FILE"

    echo ""
    echo "Step 5 ($DEVICE_ID): Verifying installation..."
    if "$ADB_CMD" -s "$DEVICE_ID" shell pm list packages | tr -d '\r' | grep -q "$PLUGIN_PACKAGE"; then
        echo "✓ Plugin installed successfully on $DEVICE_ID"
        
        # Get version info
        VERSION_INFO=$("$ADB_CMD" -s "$DEVICE_ID" shell dumpsys package "$PLUGIN_PACKAGE" | tr -d '\r' | grep -E "versionCode|versionName" | head -2)
        echo ""
        echo "($DEVICE_ID) Version info:"
        echo "$VERSION_INFO"
        
        # Check if ATAK is installed
        echo ""
        if "$ADB_CMD" -s "$DEVICE_ID" shell pm list packages | tr -d '\r' | grep -q "com.atakmap.app.civ"; then
            echo "✓ ($DEVICE_ID) ATAK (CIV) is installed"
        else
            echo "⚠ ($DEVICE_ID) ATAK (CIV) not found - please install ATAK first"
        fi
        
    else
        echo "✗ ($DEVICE_ID) Plugin installation FAILED. Skipping further checks for this device."
    fi
done

echo ""
echo "====================================================="
echo "=== Multi-Device Installation Complete            ==="
echo "====================================================="
echo ""
echo "Plugin: $PLUGIN_NAME"
echo "Package: $PLUGIN_PACKAGE"
echo ""

echo "Next steps (for each device):"
echo "1. Open ATAK"
echo "2. Find the plugin in ATAK's plugin menu"
echo "3. Configure plugin settings if needed"
echo "4. Start using the plugin!"
echo ""
echo "Troubleshooting (for each device):"
echo "- Check 'adb -s <DEVICE_ID> logcat' for debug logs"
echo "- Filter logs by plugin name for specific messages"
echo "- Verify your device has necessary permissions"
echo "- Check that ATAK is properly installed and running"
echo "- Restart ATAK if the plugin doesn't appear"
echo ""
echo "For plugin-specific configuration, refer to the plugin's README or documentation."


