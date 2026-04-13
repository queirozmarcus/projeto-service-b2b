@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script para Windows
@REM Versão do Maven: 3.9.6
@REM Compatibilidade: Java 21, Spring Boot 3.2
@REM ----------------------------------------------------------------------------
@echo off

SET MAVEN_VERSION=3.9.6
SET MAVEN_WRAPPER_PROPERTIES=%~dp0.mvn\wrapper\maven-wrapper.properties
SET DISTRIBUTION_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

@REM Lê distributionUrl do properties se existir
IF EXIST "%MAVEN_WRAPPER_PROPERTIES%" (
  FOR /F "tokens=2 delims==" %%A IN ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') DO (
    SET DISTRIBUTION_URL=%%A
  )
)

@REM Determina diretório de cache
SET USER_HOME_DIR=%USERPROFILE%
IF "%USER_HOME_DIR%"=="" SET USER_HOME_DIR=%HOMEDRIVE%%HOMEPATH%

SET MAVEN_HOME_PARENT=%USER_HOME_DIR%\.m2\wrapper\dists
SET DISTRIBUTION_ID=apache-maven-%MAVEN_VERSION%-bin
SET MAVEN_HOME=%MAVEN_HOME_PARENT%\%DISTRIBUTION_ID%

@REM Baixa Maven se necessário
IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Baixando Maven %MAVEN_VERSION% de %DISTRIBUTION_URL%
  IF NOT EXIST "%MAVEN_HOME_PARENT%" mkdir "%MAVEN_HOME_PARENT%"
  SET DOWNLOAD_FILE=%MAVEN_HOME_PARENT%\%DISTRIBUTION_ID%.zip

  powershell -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%DOWNLOAD_FILE%'" || (
    echo ERRO: Falha ao baixar Maven.
    exit /b 1
  )

  powershell -Command "Expand-Archive -Path '%DOWNLOAD_FILE%' -DestinationPath '%MAVEN_HOME_PARENT%' -Force" || (
    echo ERRO: Falha ao extrair Maven.
    exit /b 1
  )

  IF EXIST "%MAVEN_HOME_PARENT%\apache-maven-%MAVEN_VERSION%" (
    move "%MAVEN_HOME_PARENT%\apache-maven-%MAVEN_VERSION%" "%MAVEN_HOME%"
  )
  del "%DOWNLOAD_FILE%"
)

@REM Executa Maven
SET MAVEN_EXECUTABLE=%MAVEN_HOME%\bin\mvn.cmd
IF NOT EXIST "%MAVEN_EXECUTABLE%" (
  echo ERRO: Maven executavel nao encontrado em %MAVEN_EXECUTABLE%
  exit /b 1
)

"%MAVEN_EXECUTABLE%" %*
