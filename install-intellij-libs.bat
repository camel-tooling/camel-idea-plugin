@echo off
:: This script will install all files in IntelliJ IDEA's lib/ folder to the local maven .m2 repository.
:: This way we can use them during the build.

:: Usage:
::   install-intellij-libs.bat 2017.1 "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2017.1"

set IDEA_VERSION=%1
set INTELLIJ_HOME=%2

:: Remove quote characters
set INTELLIJ_HOME=%INTELLIJ_HOME:"=%

if "%INTELLIJ_HOME%"=="" (
  echo Please provide the version and path to the IntelliJ home directory.
  echo For example:
  echo install-intellij-libs.bat 2017.1 "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2017.1"
  exit /b
)

if not exist "%INTELLIJ_HOME%" (
  echo Directory does not exist: "%INTELLIJ_HOME%"
  exit /b
)

echo Installing IntelliJ artifacts to Maven local repository
echo IntelliJ home: "%INTELLIJ_HOME%"
for %%i in ("%INTELLIJ_HOME%\lib\*.jar") do (
  :: %%~ni - file name (n)
  call mvn install:install-file -Dfile="%%i" -DgroupId=com.intellij -DartifactId="%%~ni" -Dversion=%IDEA_VERSION% -Dpackaging=jar
)

call mvn install:install-file -Dfile="%INTELLIJ_HOME%/plugins/properties/lib/properties.jar" -DgroupId=com.intellij.plugins -DartifactId=properties -Dversion=%IDEA_VERSION% -Dpackaging=jar
call mvn install:install-file -Dfile="%INTELLIJ_HOME%/plugins/properties/lib/resources_en.jar" -DgroupId=com.intellij.plugins -DartifactId=resources_en -Dversion=%IDEA_VERSION% -Dpackaging=jar
call mvn install:install-file -Dfile="%INTELLIJ_HOME%/plugins/yaml/lib/yaml.jar" -DgroupId=com.jetbrains.plugins -DartifactId=yaml -Dversion=%IDEA_VERSION% -Dpackaging=jar
call mvn install:install-file -Dfile="%INTELLIJ_HOME%/plugins/yaml/lib/resources_en.jar" -DgroupId=com.jetbrains.plugins -DartifactId=yaml-resources_en -Dversion=%IDEA_VERSION% -Dpackaging=jar
