
## generate app screenshots 

Prerequisites:

- This globals settings need to be added
    PEP_TEST_EMAIL_ADDRESS
    PEP_TEST_EMAIL_PASSWORD
    PEP_TEST_EMAIL_SERVER

    Example:
    export PEP_TEST_EMAIL_ADDRESS=account@server.com
    export PEP_TEST_EMAIL_PASSWORD=password
    export PEP_TEST_EMAIL_SERVER=server.com

- A k9 settings should be added to the device downloads folder with name "stubAccount.k9s"

- A key file should be added to the device downloads folder with name "test_key.asc"

- Change BOT_1_NAME, BOT_2_NAME & BOT_3_NAME in AccountSetupScreenshotTest.kt to your liking

- Make sure that no other device has the same account instantiated and is able to start sync

- At the time of sync test, the device will wait until the sync popup comes, so set the same account in other device for sync to start

Command:
   ./gradlew generateScreenshots


## AppRestrictions test 

- 1 Uninstall pEp app

- 2 install https://pep-security.lu/gitlab/francisco/apprestrictionenforcer

- get adb users: 
   adb shell pm list users

- example :
    Users:
        UserInfo{0:Owner:13} running
        UserInfo{10:AppRestrictionEnforcer:30} running <- use the one the has AppRestrictionEnforcer

- add argument:
    --user <user_id> 
   
- command:    
    ./gradlew testRestrictions -Puser=<user_id>

