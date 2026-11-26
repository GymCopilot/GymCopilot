[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "Process")
& .\gradlew --stop
& .\gradlew :shared:jsBrowserDevelopmentRun
