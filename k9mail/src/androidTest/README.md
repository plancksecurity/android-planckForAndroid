## Running instrumentation tests (excluding screenshots and AppRestrictions ones)

### Running from IDE

* Run Cucumber instrumentation tests: Edit configurations -> Instrumentation Arguments -> Add String parameter `testType` with value `cucumber` under "instrumentation extra params".
* Run screenshots tool: Edit configurations -> Instrumentation Arguments -> Add String parameter `testType` with value `screenshot` under "instrumentation extra params".
* Run "plain" Espresso instrumentation tests : Nothing needed, just run normally.

### Running from command line
* Run Cucumber instrumentation tests: `./gradlew cucumberTest`
* Run "plain" Espresso instrumentation tests : `./gradldew connectedCheck`
* Run all instrumentation tests (both "plain" Espresso and Cucumber instrumentation tests): `./gradldew connectedCheckAll`

### Running any instrumentation tests from gradle on release build
* `./gradlew -PtestBuildType="release" <test task>`

### Running on already installed app
* Command: `./gradlew customTest`. 
* Applicable project properties:
  * `-PtestBuildType` (build type for tests, "release" or "debug". Default `debug`)
  * `-Pflavor` (build variant or flavor. Default `enterprise`)
  * `-Pwork` (whether to run tests on work profile. Default `false`)
  * `-PuseFakeManager` (whether to use FakeRestrictionsManager for the tests. Default `false`)
  * `-Pdevice` (which device to run tests on, when we have several devices connected, result of `adb devices`. Default `null`)
  * `-PtestType` (whether to run Cucumber, Screenshots or "plain Espresso" tests. Options are `normal`, `cucumber`, `screenshot`. Default `normal`)
  * `-Pverbose` (more verbose output. Default `false`)

* Example: `./gradlew customTest -PtestBuildType="release" -Pflavor="enterprise" -Pwork=true -PuseFakeManager=false -Pdevice="1f77616" -PtestType="cucumber" -Pverbose=true`


## generate app screenshots 

Prerequisites:

- This globals settings need to be added
    PLANCK_TEST_EMAIL_ADDRESS
    PLANCK_TEST_EMAIL_PASSWORD
    PLANCK_TEST_EMAIL_SERVER

    Example:
    export PLANCK_TEST_EMAIL_ADDRESS=account@server.com
    export PLANCK_TEST_EMAIL_PASSWORD=password
    export PLANCK_TEST_EMAIL_SERVER=server.com

- Make sure that no other device has the same account instantiated and is able to start sync

- At the time of sync test, the device will wait until the sync popup comes, so set the same account in other device for sync to start

Command:
   ./gradlew generateScreenshots