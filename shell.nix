{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    # Java
    jdk17

    # Android SDK
    android-tools  # provides adb, fastboot

    # Build tools
    gradle

    # Kotlin linting
    ktlint

    # Markdown linting
    nodePackages.markdownlint-cli
  ];

  shellHook = ''
    echo "Magic App dev shell ready."
    echo "  adb:            $(adb version 2>&1 | head -1)"
    echo "  java:           $(java -version 2>&1 | head -1)"
    echo "  markdownlint:   $(markdownlint --version)"
  '';
}
