{ pkgs ? import <nixpkgs> {} }:

let
  androidComposition = pkgs.androidenv.composeAndroidPackages {
    buildToolsVersions = [ "34.0.0" ];
    platformVersions = [ "29" ];
    includeEmulator = false;
    includeSystemImages = false;
  };
  androidSdk = androidComposition.androidsdk;
in
pkgs.mkShell {
  buildInputs = with pkgs; [
    jdk17
    gradle
    androidSdk
    ktlint
    nodePackages.markdownlint-cli
  ];

  ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
  ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
  JAVA_HOME = "${pkgs.jdk17.home}";

  shellHook = ''
    export PATH="${androidSdk}/libexec/android-sdk/platform-tools:$PATH"
    echo "Magic App dev shell ready."
    echo "  ANDROID_SDK_ROOT: $ANDROID_SDK_ROOT"
    echo "  java: $(java -version 2>&1 | head -1)"
    echo "  markdownlint: $(markdownlint --version)"
  '';
}
