:: occt-java
:: SET OCCT_BASE_DIR=D:\05_Software_Tools\OpenCASCADE-7.4.0-vc14-64
:: JDK location
SET JAVA_PATH=C:\Program Files\AdoptOpenJDK\jdk-11.0.9.101-hotspot
:: Maven location
SET MAVEN_DIR=D:\12_Dev\ToolsAndLibs\maven\apache-maven-3.6.3\bin
:: SWIG location
SET SWIG_BASE_DIR=D:\12_Dev\ToolsAndLibs\swigwin-4.0.2

:: Set the Generator and Plattform
SET BUILD_GENERATOR=Visual Studio 14 2015
SET GENERATOR_PLATFORM=x64

:: OpenCascade
:: SET OCCT_DIR=%OCCT_BASE_DIR%\opencascade-7.4.0
:: Nur den Pfad zum root der kompilierten Dateien, nicht zu den einzelnen Verzeichnissen (bin und lib)
:: SET OCCT_LIB_DIR=%OCCT_DIR%\win64\vc14

SET SWIG_DIR=%SWIG_BASE_DIR%\Lib
SET SWIG_EXECUTABLE=%SWIG_BASE_DIR%\swig.exe

:: Requirements
:: SET THIRD_PARTY=%OCCT_BASE_DIR%
:: SET FREETYPE=%THIRD_PARTY%\freetype-2.5.5-vc14-64
:: SET FREEIMAGE=%THIRD_PARTY%\freeimage-3.17.0-vc14-64
:: SET FFMPEG=%THIRD_PARTY%\ffmpeg-3.3.4-64
:: SET TBB=%THIRD_PARTY%\tbb_2017.0.100

SET WIN_SDK=C:\Program Files (x86)\Windows Kits\8.1

cd native\swt-cef
mkdir build
cd build

:: config
cmake -G"%BUILD_GENERATOR%" -A%GENERATOR_PLATFORM% ^
 -DSWIG_DIR="%SWIG_DIR%" ^
 -DSWIG_EXECUTABLE="%SWIG_EXECUTABLE%" ^
 -DJDK_PATH="%JAVA_PATH%" ^
 -DWINSDK_DIR="%WIN_SDK%" .. || exit /b

:: build
cmake --build . --config Release --target install

cd ..
rmdir /s /q build
cd ../..

:: Mavenize everything
:: cd java
:: call "%MAVEN_DIR%\mvn" -version
:: call "%MAVEN_DIR%\mvn" clean
:: call "%MAVEN_DIR%\mvn" versions:set -DnewVersion=0.0.1-SNAPSHOT-win
:: call "%MAVEN_DIR%\mvn" package
:: call "%MAVEN_DIR%\mvn" source:jar install
:: call "%MAVEN_DIR%\mvn" exec:java -Dexec.mainClass="de.ict.Test"
:: call "%MAVEN_DIR%\mvn" exec:java -Dexec.mainClass="de.ict.OcctTest" -Dexec.args="<path to step model>"
:: cd ..