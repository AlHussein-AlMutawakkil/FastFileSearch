@echo off
chcp 65001 > nul
cd /d "%~dp0\.."

echo [FastFileSearch] Compiling License Generator...
javac -encoding UTF-8 license_tools\KeyGen.java license_tools\LicenseGeneratorGui.java
if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %errorlevel%
)

echo [FastFileSearch] Launching License Generator GUI...
start "" javaw -cp . license_tools.LicenseGeneratorGui
exit /b 0
