Feature: Attachment

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test

  @scenario
  Scenario: Misc SearchInbox
  Description: Search for an item in your inbox
  Assumptions: You have at least 50 items in your inbox
  Expectation: Outlooks finds the string you are looking for in encrypted messages.
  Steps for Testing
  •	Look for a string in your inbox that is not in every message.
  •	Enter the search term in the “Search Current Mailbox” search field and press enter
  •	VERIFY if Outlook found the item you are looking for.
  •	Repeat steps 1-3 for 4 other search terms.

  @scenario
  Scenario: Misc SearchSentItems
  Description: Search for an item in your SENT items
  Assumptions: You have at least 50 items in your SENT folder
  Expectation: Outlooks finds the string you are looking for in encrypted messages.
  Steps for Testing
  •	Look for a string in your SENT items that is not in every message.
  •	Enter the search term in the “Search Current Mailbox” search field and press enter
  •	VERIFY (1.2.23_01) if Outlook found the item you are looking for.
  •	Repeat steps 1-3 for 4 other search terms.

  @scenario
  Scenario: Misc MoveToSubfolderAndSearch
  Description: Create a new subfolder in Outlook and move an encrypted message to the subfolder.
    Once the message is moved, go back to the inbox and search for the message.
  Assumptions: You have at least encrypted message in your inbox
  Expectation: Moving the message to the subfolder doesn’t cause issues.
    Outlook finds the message when search for it.
  Steps for Testing
  •	Create a new subfolder for your Inbox.
  •	Move an encrypted message (“Secure…” or “Secure & Trusted”) to the subfolder
  •	Go to the subfolder and VERIFY (1.2.24_01) if the message is there and the Privacy Status didn’t change.
  •	Remember a word in the moved message and go back to the Inbox.
  •	Search for the word that you remember from the previous step
  •	VERIFY (1.2.24_02) if pEp found the message in the subfolder


  @scenario
  Scenario:  ***** As of September 2017 this is not implemented this will only be fully supported with the new message format.
  Description: Ensure pEp sends separate message to BCC contacts. This is necessary, to really keep the BCC contacts hidden.
  Assumptions: Public keys of at least two communication partners are available. One of these is test006@pep-security.net
  Expectation: pEp encrypts the message for the BCC contact separately.
  Steps for Testing
  •	Open a new message
  •	Enter a “BCC test: Message only encrypted with To contacts?” and some body
  •	Add test006@peptest.ch as recipient in the “To” field
  •	Add another recipient to the “BCC” field
  •	Make sure the Privacy Status is either “Secure…” or “Secure & Trusted”
  •	Send the message
  •	Someone of the pEp team has to manually verify if the message was encrypted with the correct keys.
    Wait for the answer (may takes 1-2 hours, proceed with other tests)
  •	pEp team will inform you, if the test succeeded.

  @scenario
  Scenario: Misc LocalDistributionListPublicKeyAvailable
  Description: When sending a message to a distribution list, pEp has to check if it has a public key for all recipients.
    If this is the case, it has to encrypt the message for all recipients.
  Assumptions: A distribution list / contact group is in the address book of “Contacts (This computer only)”, not an address book of an Account.
    If the distribution list is in another folder, this test case will fail.
    A public key is available for all members for the distribution list.
  Expectation: pEp encrypts the message and all recipients can read it.
  Steps for Testing
  •	Create a new message
  •	Add a distribution list / contact group in the “To” field. Do NOT expand the list.
  •	Enter a subject and body
  •	Send the message
  •	VERIFY in SENT items if the message has been sent encrypted
  •	VERIFICATION: Ask a communication partner from the contact list if he received the message and if it was encrypted.

  @scenario
  Scenario: Misc ExchangeDistributionList
  Description: When sending a message to an Exchange distribution list, pEp has to check if it has a public key for all recipients.
    If this is the case, it has to encrypt the message for all recipients.
  Assumptions: An Exchange distribution list is in the address book.
    A public key is available for all members for the distribution list.
  Expectation: pEp encrypts the message and all recipients can read it.
  Steps for Testing
  •	Create a new message
  •	Add a distribution list / contact group in the “To” field. Do NOT expand the list.
  •	Enter a subject and body
  •	Send the message
  •	VERIFY in SENT items if the message has been sent encrypted
  •	VERIFICATION: Ask a communication partner from the contact list if he received the message and if it was encrypted.

  @scenario
  Scenario: Misc SaveMessageOnFileSystem
  Description: Save an encrypted message you receive on your file system. The message should be saved unencrypted
  Assumptions: None
  Expectation: The message saved on the file system is unencrypted.
  Steps for Testing
  •	Ask a communication partner to send you an encrypted message with an attachment.
  •	Once the message is received, open it.
  •	Click File -> Save As and save the message to the Desktop
  •	Close Outlook.
  •	VERIFY if you can open the message (double click the files on the Desktop). Also open the attachment in the message.

  @scenario
  Scenario: Misc PassiveMode
  Description: When pEp is in passive mode, it sends outgoing messages to unknown communication partners without a key attached.
    Only if it detect that the communication partner also uses pEp (by the pEp header field),
    it will attach a public key in outgoing messages. When a public key is received, it will be imported
  Assumptions: Passive Mode is enabled (see below for details).
  There is a communication partner using pEp with Passive Mode disabled. You don’t have the public key of this communication partner.
  Expectation: When the communication partner sends a message with a pEp header, the reply to the communication partner will contain the public key.
    Once the public key of the communication partner is available, pEp will encrypt
  How to enable Passive Mode:
  •	Close Outlook
  •	Open the Windows Registry by clicking “Start” and typing “regedit” and press Enter
  •	Browse to HKEY_CURRENT_USER\SOFTWARE\pEp\Outlook.
  •	Click Edit -> New -> String Value
  •	Enter the Name “IsPassiveModeEnabled”
  •	Right click the new Item and choose Modify…
  •	Then enter “True” and click OK
  •	Close the Registry Window
  •	Open Outlook
  How to disable Passive Mode
  •	Close Outlook
  •	Open the Windows Registry by clicking “Start” and typing “regedit” and press Enter
  •	Browse to HKEY_CURRENT_USER\SOFTWARE\pEp\Outlook.
  •	Right click item “IsPassiveModeEnabled” and choose Modify…
  •	Then enter “False” and click OK
  •	Close the Registry Window
  •	Open Outlook
  Steps for Testing
  •	Enable Passive Mode (or make sure it is enabled)
  •	Create a new email and enter the email address of a new communication partner user6 and enter some random subject and body
  •	VERIFY that the Privacy Status changes to “Unsecure”
  •	Send the email
  •	After the reply arrived, VERIFY if the message was sent “Unsecure”.
  •	VERIFY if in the reply message the Privacy Status is “Secure…”
  •	Disable Passive Mode

  @scenario
  Scenario: Misc UnprotectedMessageSubjects
  Description: Test the pEp option “unencrypted message subjects”
  Assumptions: A public key of at least of communication partner is available
  Expectation: Messages sent when “unencrypted message subjects” is enabled, do not encrypt the message subject
  Steps for Testing
  •	In Outlook, go to “File” -> “pEp” -> “Accounts”, tick “Enable unencrypted message subjects” and click “OK”
  •	Create a new message and enter a recipient (you have a public key), subject and body. The subject should be “This is an unencrypted subject”
  •	Make sure the Privacy Status changes to “Secure…” or “Secure & Trusted”
  •	Send the message and wait for the reply
  •	In the reply, VERIFY if the subject of the original message has been encrypted.
  •	After the test, disable the setting “Enable unencrypted message subjects” again

  @scenario
  Scenario: Misc UseKeyserver
  Description: Test the pEp option “Look up keys on key server”
  Assumptions: test003@peptest.ch public key is uploaded to key server.
  You don’t have the key of user test003@peptest.ch in your key ring yet.
  Expectation: Message should be sent encrypted. The key is automatically loaded from the key server
  Steps for Testing
  •	In Outlook, go to “File” -> “pEp” -> “Compatibility”, tick “Look up keys on key server” and click “OK”
  •	Create a new message and enter a recipient test003@peptest.ch, a subject and a body.
  •	Make sure the Privacy Status changes to “Unsecure”. In case it changes to “Secure…”,
    you already have the public key of this user test003@peptest.ch on this system and cannot perform this test with user test003@peptest.ch
  •	Send the message
  •	VERIFY in SENT items, if the message has been sent encrypted “Secure…”
  •	After the test, disable the setting “Look up keys on key server” again.

  @scenario
  Scenario: Misc MasterKey
  Description: Encrypt all outgoing messages with an additonal Master Key.
  Assumptions: Public key of test006@peptest.ch is in Key Store
  Expectation: All outgoing messages are encrypted with the Master Key
  Steps for Testing
  •	In Outlook, go to File -> pEp -> Compatibilty -> Advanced -> Open Key Manager
  •	Write down the Key ID of a Public Key (only the blue key icon, not yellow key). A Key ID has the following format: 5EC661C0.
    Don’t take the Key ID of your own account.
  •	Close Key Manager and pEp Options
  •	Close Outlook
  •	Open the Windows Registry by clicking “Start” and typing “regedit” and press Enter
  •	Browse to HKEY_CURRENT_USER\SOFTWARE\pEp\Outlook.
  •	Open the String Value “ExtraKeys” (or create if it doesn’t exist yet)
  •	Enter the Key ID from above to the value field of the String Value ExtraKey.
  •	Close the registry and open Outlook
  •	Create a new message and enter the email address of an existing test bot user.
  •	Enter some body & subject
  •	Verify if the Privacy Status is either “Secure…” or “Secure & Trusted”
  •	Send the message
  •	Wait for the reply of the test bot and verify in the body, if the initial message was also encrypted with the Master Key

  @scenario
  Scenario: Misc BlacklistingAddUser
  Description: In order not to use a key of a PGP user anymore, it can be blacklisted.
  This test case checks if blacklisted keys are ignored (not used to encrypt outgoing messages).
  Assumptions: At least one communication partners public key is available (e.g. user1)
  Expectation: Key can be searched to blacklist.
    Key can be added to blacklist.
    Message is sent unencrypted if Key is on blacklist.
  Steps for Testing
  •	In Outlook click File -> pEp -> Compatibility -> Advanced
  •	Tick the box next to the name of your communication partner that you want to blacklist (e.g. user1).
  •	Click OK to close the pEp Options
  •	Create a new message
  •	Enter the email address of the contact, whose key you just added to the blacklist
  •	Enter some subject and body
  •	VERIFY if the Privacy Status is “Unsecure”
  •	Send the message
  •	VERIFY in SENT items, if the message has Privacy Status “Unsecure”

  @scenario
  Scenario: Misc BlacklistingRemoveUser
  Description: Remove a key from the blacklist and make sure the message is sent encrypted
  Assumptions: At least one key is on the blacklist (e.g. user1 from previous test)
  Expectation: The key is removed from the blacklist.
    Messages to the contact of the formerly blacklisted keys are encrypted.
  Steps for Testing
  •	In Outlook click File -> pEp -> Compatibility -> Advanced
  •	Untick the box next to the key that you want to remove from the blacklist.
  •	Create a new message and enter the email address of the contact to whom the public key belongs.
  •	Enter a subject and body
  •	VERIFY if the Privacy Status of the message is “Secure…” or “Secure & Trusted”
  •	Send the message
  •	VERIFY in SENT items, if the message has Privacy Status “Secure…” or “Secure & Trusted”

  @scenario
  Scenario: Misc UnencryptedForwardWarningFlag
  Description: If a formerly encrypted message (that I received) is forwarded unencrypted, a warning message appears.
  Assumptions: An encrypted message is in the inbox
  Expectation: A warning message appears, when the message is forwarded unencrypted.
  Steps for Testing
  •	In Outlook go to File -> pEp -> Accounts -> Advanced and enable “Show a warning when a message loses security through reply or forward”
  •	Send a message to user1 and wait for the reply
  •	VERIFY if the reply was encrypted (Secure or Secure & Trusted)
  •	Forward this message and enter a recipient that you don’t have the public key (e.g. user7)
  •	VERIFY if the privacy status changes to “Unsecure”
  •	Send the message
  •	Verify if a warning message appears
  •	Select “Yes” to send the message

  @scenario
  Scenario: Misc RenewExpiredKey (1/3)
  Description: If a key is expired, it should automatically be renewed 10 days before expiration.
  Assumptions: The key expiration date is < 10 days (for simulation, this can be changed manually)
  Expectation: The key is updated and attached to all outgoing messages.
  Steps for Testing
  •	Set the expiry date of your private key to any date within the next 10 days. This can be done in GPA.
  •	Close Outlook and open it again
  •	Send an email to a communication partner
  •	VERIFY (1) in GPA, if the expiry date of the key has been changed. The new expiry date should be today+365 days.

  @scenario
  Scenario: Misc RenewExpiredKey (2/3).Import updated key of other users
  Description: When another user updates his key, this key should be imported to the key store when receiving a message from that user
  Assumptions: An existing communication partner (you already have the public key in your key store) updates the expiry date of a key.
  Expectation: The updated key is imported to the key store
  Steps for Testing
  •	Ask an existing communication partner (you already have the public key in your key store) updates the expiry date of the own key
  •	The communication partner sends you an email with pEp
  •	Open the message of the communication partner
  •	VERIFY (1) in GPA, if the expiry date of the key of the communication partner has been updated.

  @scenario
  Scenario: Misc RenewExpiredKey (3/3). Store Protected. This test case only applies to trusted servers.
  Description: The Store Protected feature provides the sender of an email the possibility to ensure that a message will not be saved unencrypted on the server of the receiving communication Partner.
    This is an enterprise feature.
  Assumptions: Two communication partners. Both are working on trusted servers.
  Expectation: On trusted servers, the message will never be stored unencrypted on the server.
  Steps for Testing
  •	Ask your communication partner to activate the setting “Show store protected option” in Accounts.
  •	Then, your communication partner should create a new message, add you to the recipients, add some subject and text and
    activate the “Store Protected” option. Then, the communication partner should send the message.
  •	When you receive the message, VERIFY (1) if you can read the message (decrypted) and the Privacy Status is either Secure or Secure & Trusted
  •	Then, go to the webmail of your email account and check if you can read the same message in the Webmail.
    If you can read the message, this test failed. If the message is still encrypted, this test passed.
  •	Validation Steps

  @scenario
  Scenario: DisabledProtection SendMessageNoKeyAvailable
  Description: With Privacy Protection disabled pEp will not attach a public key to outgoing messages.
  Assumptions: No messages have been exchanged with user8 yet.
  Expectation: The test bot will reply unencrypted because no public key was attached
  Steps for Testing
  For the current account, make sure “Enable pEp Privacy Protection” is disabled and “Continue to decrypt messages” is still enabled. In pEp for Outlook it will look like this:
  •	Create a new message
  •	Enter the email address of user8 to the To field. Enter some subject and body
  •	VERIFY that the Privacy Status bar is not displayed
  •	Send the message
  •	Validation Steps

  @scenario
  Scenario: DisabledProtection ReplyOnUnencryptedMessage
  Description: Reply on the unencrypted message from the previous test case
  Assumptions: You have a message from user8 of the previous test case.
    Do not change the pEp settings from the previous test case
  Expectation: The reply will not be encrypted
  Steps for Testing
  •	Open the message of the previous test case and reply
  •	VERIFY that the Privacy Status bar is not displayed
  •	Send the message

  @scenario
  Scenario: DisabledProtection SendMessageWithEnableProtection
  Description: Create a new message to user9 and “Enable Protection” and make sure the public key has been attached to the outgoing message.
  Assumptions: No messages have been exchanged with user9 yet.
    Do not change the pEp settings from the previous test case.
  Expectation: Because no key of user9 is available at this point, the outgoing message will not be encrypted.
    A public key will be attached to the outgoing message. Because of this the test bot replies encrypted.
  Steps for Testing
  •	Create a new message
  •	Enter the email address of user9 in the To field and enter some subject and body
  •	Click “Enable Protection”
  •	VERIFY that the Privacy Status bar appears and shows Privacy Status “Unsecure”
  •	Send the message
  •	Wait for the reply of the test bot
  •	VERIFY that the Privacy Status of the reply is “Secure”

  @scenario
  Scenario: DisabledProtection ReplyOnEncryptedMessage
  Description: When replying on an encrypted message the outgoing message will be encrypted.
  Assumptions: The reply message from the previous test case was encrypted.
    Do not change the pEp settings from the previous test case.
  Expectation: The reply on the encrypted message is encrypted
  Steps for Testing
  •	Click on the message of the previous test case
  •	VERIFY (1.3.4_01) that the privacy status is “Secure”
  •	Reply to the message
  •	Enter some body
  •	VERIFY (1.3.3_02) that the Privacy Status is “Secure”
  •	Send the message

  @scenario
  Scenario: DisabledProtection CreateNewMessageToContactWithKeyAvailable
  Description: When creating a new message to a contact where a key is already available the message will be sent unencrypted.
  Assumptions: The previous test case with user9 passed
  Expectation: The new message sent to user9 will be unencrypted.
  Steps for Testing
  •	Create a new message
  •	Enter the email address of user9 in the To field.
  •	Add some subject and body
  •	VERIFY that the Privacy Status bar is not visible
  •	Send the message
  •	Go to the Sent items and click on the message you just sent.
  •	VERIFY that the Privacy Status bar is not visible

  @scenario
  Scenario: DisabledProtection ReceiveMessageWithContinueToDecryptMessagesDisabled
  Description: When “Continue to decrypt messages” is disabled, incoming messages will not be decrypted.
  Assumptions: A public key of user9 (previous tests) is available.
  Expectation: Incoming messages that are encrypted will not be decrypted
  Steps for Testing
  •	Create a new message
  •	Enter the email address of user9 in the To field.
  •	Add some subject and body
  •	Select “Enable Protection”
  •	VERIFY that the Privacy Status bar appears and the Privacy Status is “Secure”
  •	Send the message
  •	Wait for the reply of the test bot
  •	VERIFY that the message that has just been received cannot be read.

  @scenario
  Scenario: Basic Conversation with Reader
  Description: Test simple conversation: incoming messages are decrypted and outgoing messages are sent unencrypted with public key attached.
  Assumptions: pEp for Outlook Reader is installed. Communication partner has pEp installed (full version)
  Expectation: Incoming messages are decrypted, outgoing messages are unencrypted
  Steps for Testing
  •	Send a message to a communication partner
  •	Ask communication partner (that uses the full version of pEp for Outlook) to reply to this message
  •	VERIFY (1), if the incoming message is encrypted. pEp Reader for Outlook will decrypt the message. Privacy Status should be Secure.
  •	Reply to the message and add some text
  •	VERIFY (2), if the Privacy Status is Unsecure
  •	VERIFY (3) if the links to the shop to but the full version, work
  •	Send the message. In the Splash Screen confirm that you want to send the message.
  Scenario: Upgrade pEp Reader to Full version
  Description: Upgrade pEp to full version and verify if outgoing messages are now encrypted
  Assumptions: pEp for Outlook Reader is installed, pEp for Outlook Reader has been used previously (e.g. previous test)
  Expectation: After the upgrade, outgoing messages are encrypted
  Steps for Testing
  •	Open Outlook, send reply to a previously received message, that was encrypted
  •	VERIFY (1), in the Splash screen, click buy pEp for Outlook. The link should take you to the shop.
  •	Run setup of pEp for Outlook (full version, as provided for the basic test cases)
  •	After the setup, start Outlook, and reply to the same message as in 1.
  •	VERIFY (2), if the outgoing message is sent Secure or Secure & Trusted.
  Scenario: pEp for Android: Setup
  The goal of these test cases is, to ensure the installation of pEp for Android works without issues.
  Requirements: Android 5 or later. Installation with Outlook closed
  Description: Test if the installation of pEp for Android runs without issues.
  Assumptions: Android. pEp for Android APK on the device
  Expectation: After the installation and setup, you can send an receive messages
  Steps for Testing
  •	Install pEp by opening the APK
  •	Open pEp and setup a new account
  •	Send a test message
  •	VERIFY with the communication partner if the message arrived.
  Scenario: Validation Steps
  Description: Test if the update of pEp for Android runs without issues. Also ensure that configurations are not lost
  Assumptions: Before you start this test, reset the VM
  Expectation: After the installation, pEp works and the configuration is kept
    During the installation, no error message appears.
    Ask the communication partner if the message arrived
    Update APK / Review configurations after update
  Steps for Testing
  •	Install an old version of pEp for Android and open it
  •	Setup a new account
  •	In Settings -> Global Settings -> pEp change the following settings
  •	Passive Mode: Enabled
  •	Unprotected message subject: Enabled
  •	Close pEp
  •	Run the setup of the new pEp for Android version (at least 1.0)
  •	After the setup, open pEp
  •	VERIFY (2), if the settings you configured in 3. are still unchanged
  •	VERIFY (3), if the pEp version umber has changed

  @scenario
  Scenario: Adding a second device (1/3)
  Description: The user already has pEp in use with one product (e.g. pEp for Outlook). The user adds a second device with the (e.g. pEp for Outlook).
  Assumptions: The user has pEp setup on one device
  Expectation: The second device receives and imports the key of the first device, and the other way around. When sending new messages, both devices use the same key.
  Steps for Testing
  •	On the second device add an account and then install pEp (or the other way around. Both to be tested)
  •	On both devices the screen “Add to device group” (or similar) should appear. Confirm the Trustwords on both devices and confirm (if they are the same).
  •	Send a message from the first device to another user (we call him Bob).
  •	Ask Bob to reply encrypted and try to read the message on the second device.
  •	Reply to Bob encrypted
  •	Bob should verify, if the keys in both messages (from the first and second device) are identical

  @scenario
  Scenario: Adding a second device while first device is offline (2/3)
  Description: The user already has pEp in use with one product (e.g. pEp for Outlook). The user adds a second device with the (e.g. pEp for Outlook).
  Assumptions: The user has pEp setup on one device
  Expectation: The second device receives and imports the key of the first device, and the other way around. When sending new messages, both devices use the same key.
  Steps for Testing
  •	From the first device (that is already set up) send an email to another user (let’s call him Bob)
  •	Ask Bob to reply encrypted and make sure you can read it on your first device
  •	Take your first device offline (e.g. close Outlook)
  •	On the second device add an account and then install pEp (or install pEp first and the setup the account and restart Outlook. Both to be tested)
  •	Send a message to Bob from the second device
  •	Ask Bob to reply (encrypted)
  •	On the second device open the message. It should be possible to decrypt.
  •	Then, go online on the first device (e.g. open Outlook)
  •	On both devices the screen “Add to device group” (or similar) should appear. Confirm the Trustwords on both devices and confirm (if they are the same).
  •	Send a message from the first device to another user Bob.
  •	Ask Bob to reply encrypted and try to read the message on the second device.
  •	Reply to Bob encrypted
  •	Bob should verify, if the keys in both messages (from the first and second device) are identical

  @scenario
  Scenario: Adding another device (3/3)
  Description: The user already has a device group (with at least two devices). The user adds another device.
  Assumptions: The user has device group according to the list above.
  Expectation: The new device receives and imports all private keys of the existing device group. When sending new messages, all devices use the same key.
  Steps for Testing
  •	On the third device add an account and then install pEp (or the other way around. Both to be tested)
  •	On the new and one of the existing devices the screen “Add to device group” (or similar) should appear. Confirm the Trustwords on both devices and confirm (if they are the same).
  •	Send a message from the first device to another user (we call him Bob).
  •	Ask Bob to reply encrypted and try to read the message on the third device.
  •	Reply to Bob encrypted
  •	Bob should verify, if the keys in both messages (from the first and third device) are identical

  @scenario
  Scenario: Expired key renewed and synced across all devices
  Description: The user has a device group with two devices. The current key expires and should be renewed and synced across all devices.
  Assumptions: The device group is setup and the key expired
  Expectation: Only one new key is generated. The new key is synced across all devices
  Scenario:: Key with passphrase
  Expectation: Outlook to Outlook: Key sync works as normal. New client is Android: pEp will show a message, that this key cannot be synced (because passphrase is not supported on Android)
  To be clarified. Will this actually work? Or is the device group key always without a passphrase?
  Compatibility with other solutions
  For following solutions compatibility needs to be checked:
  •	Thunderbird / Enigmail
  Configured on 192.168.1.223 with address pep.thunderbird@peptest.ch
  •	Apple Mail
  to be tested on Macbook
  •	Mailvelope
  Mailvelope covers a large numbers of providers including GMX, web.de, mail.google, mail.live.com, mai.yahoo.com. To test Mailvelope, an account is configured on 192.168.1.223 with Gmail and address pep.mailvelope@gmail.com (Password: pEpdichauf).
  Checks to be performed:
  •	Sending encrypted message
  •	Is signature recognized
  •	If the interface a solution to import keys automatically: Is it as easy as possible?
  •	Receiving encrypted message
  •	Is attached public key imported automatically
  •	Is signature recognized