@ECHO OFF
SET DIR=%~dp0
SET WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Gradle wrapper JAR is missing at %WRAPPER_JAR%. Run gradle wrapper on a machine with Gradle installed to finish bootstrap.
  EXIT /B 1
)

java -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
