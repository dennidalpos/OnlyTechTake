. .\scripts\powershell\support\android_env.ps1
Set-AndroidCliEnvironment
.\gradlew.bat --no-configuration-cache :app:connectedFullDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.onlytechtake.repairapp.data.repositories.RepositoryPathsInstrumentedTest"
