
## generate app screenshots 

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

