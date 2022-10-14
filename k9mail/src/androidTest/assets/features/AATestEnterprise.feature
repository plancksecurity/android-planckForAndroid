Feature: Test
  Background:
    Given I created an account


  @QTR-2354
  Scenario: Cucumber Enterprise Email Settings
    When I set account_description setting to newUser
    And I set account_display_count setting to 50
    And I set max_push_folders setting to 30
    And I set account_remote_search_num_results setting to 20
    And I set incoming settings to server peptest.ch, securityType SSL/TLS, port 993 and userName elUser
    And I set outgoing settings to server peptest.ch, securityType STARTTLS, port 587 and userName elUserOUT
    Then I compare account_description setting with newUser
    And I compare account_display_count setting with 50
    And I compare max_push_folders setting with 30
    And I compare account_remote_search_num_results setting with 20
    And I compare incoming settings with server peptest.ch, securityType SSL/TLS, port 993 and userName elUser
    And I compare outgoing settings with server peptest.ch, securityType STARTTLS, port 587 and userName elUserOUT


  @QTR-2345
  Scenario: Cucumber Enterprise Disable Warning

    When I set unsecure_delivery_warning setting to false
    And I click compose message
    And I enter 3 unreliable recipients in the messageTo field
    Then I check insecurity warnings are not there
    When I set unsecure_delivery_warning setting to true


  @QTR-2351
  Scenario: Cucumber Enterprise TrustWords

    When I set pep_use_trustwords setting to true
    And I send 1 message to bot1 with subject TrustWordsTest and body ThisTestIsForTrustWords
    And I click the last message received
    Then I check if the privacy status is pep_green
    And I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pep_green
    When I set pep_use_trustwords setting to false


  @QTR-2344
  Scenario: Cucumber Enterprise Disable Protection

    When I set pep_enable_privacy_protection setting to false
    And I send 1 message to bot1 with subject newContact and body DisableProtectionTest
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter unsecureTest in the messageSubject field
    And I enter pEpProtectionMustBeDisabled in the messageBody field
    And I check if the privacy status is pEpRatingUnsecure
    Then I check in the handshake dialog if the privacy status is pEpRatingUnsecure
    And I click the send message button
    When I set pep_enable_privacy_protection setting to true



	#Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
	# when entering the email address of a new contact bot1.
	# Also verify if pEp attaches my public key to outgoing messages
	# Assumption: No emails with the communication partner have been exchanged so far
	# Expectation: Privacy Status of outgoing message is “Unsecure”
  @QTR-6 @QTR-1 @QTR-559
  Scenario Outline: Cucumber Mail to new contact

    When I select account <account>
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot1 in the messageTo field
    And I enter mailToNewContact in the messageSubject field
    And I enter bodyMailToNewContact in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter empty in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter bot1 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter bodyMailToNewContact2 in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with bodyMailToNewContact2
    Examples:
      | account |
      | 0       |




  @QTR-152 @QTR-1
  Scenario Outline: Cucumber Send Unencrypted email with long text

    When I select account <account>
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter TM152 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText
    Examples:
      | account |
      | 0       |



	#The long text should contain at least 15k characters.
  @QTR-153 @QTR-1
  Scenario Outline: Cucumber Send Encrypted email with long text

    When I select account <account>
    When I click compose message
    And I send 1 message to bot1 with subject sendEncrypted and body sendEncryptedTest
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter TM153 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    Then I check if the privacy status is pep_yellow
    When I go back to the Inbox
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText
    Examples:
      | account |
      | 0       |


	#[^long.txt]
	#is the link to long text
  @QTR-154 @QTR-1
  Scenario Outline: Cucumber Save Draft email with long text

    When I select account <account>
    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longText in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with longText
    And I check if the privacy status is pep_yellow
    And I discard the message
    And I go back to the Inbox

    Examples:
      | account |
      | 0       |



  @QTR-1976 @QTR-1
  Scenario Outline: Cucumber Send Encrypted email with long word

    When I select account <account>
    When I click compose message
    And I send 1 message to bot1 with subject sendEncrypted and body sendEncryptedTest
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter QTR-1976 in the messageSubject field
    And I enter longWord in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with longWord
    Then I check if the privacy status is pep_yellow
    When I go back to the Inbox
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longWord
    Examples:
      |account|
      |  0    |
      
      

  @QTR-1978 @QTR-1
  Scenario Outline: Cucumber Save Draft email with long word

    When I select account <account>
    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longWord in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with longWord
    And I check if the privacy status is pep_yellow
    And I discard the message
    And I go back to the Inbox

    Examples:
      |account|
      |  0    |
      
      




	#Scenario: Test Sanity MailFromNewContactEncrypted
	#Description: You have a new communication partner using pEp.
	#This communication partner sends you a message from pEp.
	#Verify if the public key of the communication partner is imported and you can answer encrypted.
	#Assumption: You didn’t receive any message from the communication partner so far
	#Expectation: The public key of the communication partner is imported
	#The answer to my communication partners message is encrypted
  @QTR-8 @QTR-1 @QTR-169
  Scenario Outline: Cucumber Mail from new contact encrypted

    When I select account <account>
    And I click compose message
    And I send 1 message to bot1 with subject mailFromNewContactEncryptedBody and body MailFromNewContactEncryptedBody
    And I click the last message received
    Then I compare messageBody from json file with MailFromNewContactEncryptedBody
    And I check if the privacy status is pep_yellow
    When I click reply message
    And I enter extraText in the messageSubject field
    And I enter bodyText in the messageBody field
    Then I check if the privacy status is pep_yellow
    And I click the send message button
    And I go back to the Inbox
    And I wait for the new message
    Examples:
      |account|
      |  0    |
      
      



	#Ensure mails are encrypted when pEp says so (SER-299)
  @QTR-9 @QTR-1
  Scenario Outline: Cucumber: Ensure Mails are encrypted when pEp says so

    When I select account <account>
    And I send 1 message to bot1 with subject mailsEncryptedWhenpEpSaysSo and body MailsAreEncryptedWhen_pEp_saysSo
    And I click the last message received
    And I click reply message
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I press back
    And I wait for the message and click it
    And I compare rating_string from json file with PEP_rating_reliable
    Examples:
      |account|
      |  0    |
      
      



	#Description: Test if pEp encrypts the message if you send an email to someone
	#you already have the public key from, e.g. user1.
	#Assumption: You already have a public key from your communication partner
	#Expectation: The new message should be sent encrypted
  @QTR-10 @QTR-1
  Scenario Outline: Cucumber Mail to existing contact is encrypted

    When I select account <account>
    And I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TM-10 and body TM-10
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TM-10A and body TM-10Abody
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TM-10Abody
    Examples:
      |account|
      |  0    |
      
      



	#Description: Test if the Privacy Status of a message is correct,
	#if it is sent to multiple people and the public key is available (user1 and user2)
	#Assumption: You have public keys of (at least) 2 different communication partners.
	#Expectation: The new message should be sent encrypted
  @QTR-11 @QTR-1
  Scenario Outline: Cucumber Mail to multiple contacts encrypted

    When I select account <account>
    And I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TM-11 and body TM-11
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I send 1 message to bot2 with subject TM-11A and body TM-11A
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter TM-11B in the messageSubject field
    And I enter TM-11B in the messageBody field
    And I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter bot2 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check if the privacy status is pep_yellow
    And I enter TM-11C in the messageSubject field
    And I enter TM-11CBody in the messageBody field
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TM-11CBody
    And I go back to the Inbox
    Examples:
      |account|
      |  0    |
      
      



	#Description:Test if the Privacy Status of a message is correct,
	#if it is sent to multiple people. For one of the recipients, there is no public key available.
	#Assumption: You have public keys of (at least) 2 different communication partners (user1 and user2).
	#You don’t have the key of user3.
	#Expectation: The message should be sent unencrypted
  @QTR-12 @QTR-1
  Scenario Outline: Cucumber Mail to multiple contacts (mixed)

    When I select account <account>
    And I click compose message
    And I send 1 message to bot1 with subject TM-12 and body TM-12
    And I click compose message
    And I send 1 message to bot2 with subject TM-12A and body TM-12A
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter bot5 in the messageTo field
    Then I check if the privacy status is pEpRatingUnencrypted
    When I enter TM-12B in the messageSubject field
    And I enter TM-12B in the messageBody field
    And I enter empty in the messageTo field
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter bot5 in the messageTo field
    Then I check if the privacy status is pep_red
    #This is unsecure, color resource for enterprise should be created, in the meantime we will use pep_red (this is not mistrusted color)
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_red
    #This is unsecure, color resource for enterprise should be created, in the meantime we will use pep_red (this is not mistrusted color)
    And I compare messageBody with TM-12B
    Examples:
      |account|
      |  0    |


  @QTR-2054 @QTR-1
  Scenario Outline: Cucumber Special Characters

    When I select account <account>
    And I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter Special1 in the messageSubject field
    And I enter specialCharacters in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with specialCharacters
    When I go to the sent folder
    And I click the first message
    Then I compare messageBody with specialCharacters
    When I go back to the Inbox
    And I click compose message
    And I enter myself in the messageTo field
    And I enter Special2 in the messageSubject field
    And I enter specialCharacters in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with specialCharacters

    Examples:
      |account|
      |  0    |


	#Description: Send 1 attachment to 1 contact
	# Assumption: I already have the public key of the other contact
	# and can send the message encrypted(user1)
	# Expectation: Then message is in SENT items and I can open the attachments
  @QTR-126 @QTR-3
  Scenario Outline: Cucumber Attachment Send 1 File To 1 Contact (1/2)

    When I select account <account>
    And I remove all messages
    And I select account <account>
    And I click compose message
    And I send 1 message to bot5 with subject TM-126 and body attach1
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-126A in the messageSubject field
    And I enter attachSpecialCharacters in the messageBody field
    When I attach specialCharacters
    And I click the send message button
    And I wait for the new message

    Examples:
      |account|
      |  0    |
      
      



	#Second part of Attachment Send 1 File To 1 Contact (2/2)
  @QTR-127 @QTR-3
  Scenario Outline: Cucumber Attachment Send 1 File To 1 Contact (2/2)

    When I select account <account>
    And I click the last message received
    Then I open 1 attached files
    Examples:
      |account|
      |  0    |
      
      



	#Description: Send 4 attachment to 3 contacts
	#Assumption: I already have the public key of 3 contacts and can send the message encrypted  Expectation: Then message is in SENT items and I can open the attachments
	#Steps for Testing
	# # Create a new message
	# # Enter “To…” (3 recipients you have a public key from, e.g. user1, user2 and user3), any subject and any body.
	# # Verify that the Privacy Status changes to “Secure…” or “Secure & Trusted”
	# # Attach 4 files with 4 different formats (1 picture, 1 MS Office document, 1PDF, 1 other file) to the email.
	#Make sure the total size is between 5 and 15 MB.
	# # Send the message
	# #  VERIFY if the message is in SENT items of your mailbox and you can open the attachment.
	# # Ask the recipient, if he could open all attachments
	#
	#
  @QTR-128 @QTR-3
  Scenario Outline: Cucumber Attachment Send 4 Files To 3 Contacts (1/2)

    When I select account <account>
    When I click compose message
    And I send 1 message to bot6 with subject TM-128 and body attach4
    When I click compose message
    And I send 1 message to bot2 with subject TM-128A and body attach4A
    When I click compose message
    And I send 1 message to bot5 with subject TM-128B and body attach4B
    When I click compose message
    And I enter bot6 in the messageTo field
    And I enter bot2 in the messageTo field
    And I enter bot5 in the messageTo field
    And I enter TM-128C in the messageSubject field
    And I enter attach4C in the messageBody field
    When I attach PDF
    And I attach MSoffice
    And I attach settings
    And I attach picture
    And I click the send message button
    And I wait for the new message
    Examples:
      |account|
      |  0    |
      
      



	#Second part of Attachment Send 4 Files To 3 Contacts (2/2)
  @QTR-129 @QTR-3
  Scenario Outline: Cucumber Attachment Send 4 Files To 3 Contacts (2/2)

    When I select account <account>
    And I go to the sent folder
    And I click the last message received
    Then I open 4 attached files
    Examples:
      |account|
      |  0    |
      
      


	#  Description: Receive one attachment that has only been sent to me from another pEp user  Description: Receive one attachment that has only been sent to me from another pEp user  Assumptions: The communication partner has my private key  and can send me encrypted messages with pEp.  Expectation: I can read the email and  Steps for Testing  • Ask a communication partner to send a message with 1 attachment (Privacy Status: “Secure…” or “Secure & Trusted”).  • When the message arrives, click on it.  • Make sure, the message has been sent encrypted (Privacy Status “Secure…” or “Secure & Trusted”)  • VERIFY if you can read the content of the message  • Open the attachment  • VERIFY if the attachment opens as expected
  @QTR-130 @QTR-3
  Scenario Outline: Cucumber Attachment Receive Attachments One File

    When I select account <account>
    When I click compose message
    And I send 1 message to bot5 with subject QTR-130 and body beforeAttachment
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-130 in the messageSubject field
    And I enter attach1File in the messageBody field
    And I attach PDF
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    And I compare messageBody from json file with attach1File
    And I open 1 attached files
    Examples:
      |account|
      |  0    |
      
      



	#Description: Receive three attachments that have been sent to me
	# and another person from another pEp user
	# Assumptions: The communication partner has my private key
	# and can send me encrypted messages with pEp
	# Expectation: I can read the email and attachment
	# Steps for Testing
	# # Ask a communication partner to send a message with 3 attachments encrypted to myself and another person
	# # When the message arrives, click on it.
	# # Make sure, the message has been sent encrypted (Privacy Status “Secure…” or “Secure & Trusted”)
	# # VERIFY if you can read the content of the message
	# # Open the attachment
	# # VERIFY if the attachment opens as expected
	# # Repeat 5. and 6. for each attachment
  @QTR-131 @QTR-3
  Scenario Outline: Cucumber Attachment Receive Attachments Three Files

    When I select account <account>
    When I click compose message
    And I send 1 message to bot5 with subject TM-131 and body attach3
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-131B in the messageSubject field
    And I enter attach3B in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    And I open 3 attached files
    Examples:
      |account|
      |  0    |
      

  @QTR-1961
  Scenario Outline: Cucumber Save Draft

    When I select account <account>
    And I click compose message
    And I send 1 message to bot1 with subject before and body savingTheDraft
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter saveDraft1 in the messageBody field
    And I attach MSoffice
    And I save as draft
    And I click compose message
    And I send 1 message to bot2 with subject before2 and body savingTheDraft2
    And I click compose message
    And I enter bot2 in the messageTo field
    And I enter saveDraft2 in the messageBody field
    And I attach picture
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with saveDraft2
    And I check picture is attached in draft
    And I check if the privacy status is pep_yellow
    When I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with saveDraft1
    And I check MSoffice is attached in draft
    And I check if the privacy status is pep_yellow
    And I go to the drafts folder

    Examples:
      |account|
      |  0    |


  @QTR-412
  Scenario Outline: Cucumber Search for email/s in the Inbox

    When I select account <account>
    And I select Inbox from Hamburger menu
    And I send 2 messages to bot2 with subject 3messages and body textA
    And I send 1 messages to bot5 with subject 3messages and body textC
    And I click compose message
    And I enter myself in the messageTo field
    And I enter 1messages in the messageSubject field
    And I enter textB in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I send 1 messages to bot2 with subject test and body textA
    And I send 1 messages to bot5 with subject subject and body textD
    And I click compose message
    And I enter myself in the messageTo field
    And I enter Subject in the messageSubject field
    And I enter textB in the messageBody field
    And I click the send message button
    And I wait for the new message
    Then I search for 3 messages with text 3messages
    Then I search for 1 message with text 1messages
    Then I search for 0 messages with text 0messages
    Then I search for 2 messages with text textB

    Examples:
      |account|
      |  0    |
      


  @QTR-2310
  Scenario Outline: Cucumber Calendar Event

    When I select account <account>
    And I click compose message
    And I enter myself in the messageTo field
    And I enter TM-130 in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    And I attach calendarEvent
    When I click the send message button
    And I wait for the new message

    Examples:
      |account|
      |  0    |
      


  @QTR-2311
  Scenario Outline: Cucumber Calendar Event2

    When I select account <account>
    And I click the last message received
    And I check that the Calendar is correct and body text is ThisIsTheBody

    Examples:
      |account|
      |  0    |



  @QTR-2316
  Scenario Outline: Cucumber Insecurity Warning Test

    When I select account <account>
    And I click compose message
    And I enter 3 unreliable recipients in the messageTo field

#FIXME: Only guaranteed to work with S22

    Then I check insecurity warnings are there
    And I discard the message

    Examples:
      |account|
      |  0    |


  @QTR-2319
  Scenario Outline: Cucumber Remove address clicking X button


    When I select account <account>
    And I click compose message
    And I enter myself in the messageTo field
    And I enter 3 unreliable recipients in the messageTo field
    Then I remove the 3 address clicking X button
    Then I remove the 2 address clicking X button
    Then I remove the 1 address clicking X button
    Then I check insecurity warnings are not there

    Examples:
      |account|
      |  0    |


  @QTR-2321
  Scenario Outline: Cucumber check number of Global and Account settings

    When I select account <account>
    When I check there are 2 global settings and 1 account settings
    Examples:
      | account |
      | 0       |



  @QTR-1979
  Scenario Outline: Cucumber Widget

    When I select account <account>
    And I send 1 messages to bot1 with subject WidTest and body TestingWid_gets
    Then I test widgets

    Examples:
      |account|
      |  0    |

