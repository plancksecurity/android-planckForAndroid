Feature: Test
  Background:
    Given I created an account


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
      |account|
      |  0    |
      |  1    |
      |  2    |




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
      |account|
      |  0    |
      |  1    |
      |  2    |



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
      |account|
      |  0    |
      |  1    |
      |  2    |


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
      |account|
      |  0    |
      |  1    |
      |  2    |


  @QTR-1973 @QTR-1
  Scenario Outline: Cucumber Send Unencrypted email with long words


    When I select account <account>
    And I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter myself in the messageTo field
    And I disable protection from privacy status menu
    And I enter LongWordsToMyself in the messageSubject field
    And I enter longWord in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I click the first message
    Then I compare messageBody with longWord
    When I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText
    When I go back to the Inbox
    And I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot3 in the messageTo field
    And I disable protection from privacy status menu
    And I enter LongWordsToBOT in the messageSubject field
    And I enter longWord in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with longWord
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |

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
      |  1    |
      |  2    |

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
      |  1    |
      |  2    |




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
      |  1    |
      |  2    |



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
      |  1    |
      |  2    |



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
      |  1    |
      |  2    |



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
      |  1    |
      |  2    |



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
    Then I check if the privacy status is pep_no_color
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_no_color
    And I compare messageBody with TM-12B
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |



	#Description: Send a message with disabled protection to a contact you have the public key (user1)
	#Assumption: You have public keys of (at least) 1 communication partner
	#Expectation: The message is sent unencrypted with no public key attached
	#
  @QTR-13 @QTR-1
  Scenario Outline: Cucumber Disable Protection


    When I select account <account>
    When I click compose message
    And I enter bot1 in the messageTo field
    And I disable protection from privacy status menu
    And I enter TM-13 in the messageSubject field
    And I enter TM-13 in the messageBody field
    Then I check if the privacy status is pep_no_color
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_no_color
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |


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
      |  1    |
      |  2    |


#Move to folder
  @QTR-1825
  Scenario Outline: Cucumber Move to folder

    When I select account <account>
    And I send 1 message to bot1 with subject moveThisMessage and body ThisMessageWillMoveToAnotherFolder
    And I click the last message received
    And I select from message menu refile_action
    And I select from message menu move_action
    And I select from screen notification_action_spam
    And I go back to accounts list
    When I select Spam folder of account <account>
    And I click the first message
    And I compare messageBody from json file with ThisMessageWillMoveToAnotherFolder

    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |



	#Description: Make a handshake with a communication partner
	#Assumption: You didn’t make a handshake with communication partner user1 yet
	#Expectation: Privacy Status is “Secure & Trusted” for new messages with the trusted partner
  @QTR-16 @QTR-2
  Scenario Outline: Cucumber Handshake in new Message

    When I select account <account>
    When I click compose message
    And I send 1 message to bot3 with subject TM-16 and body TM-16body
    And I click the last message received
    And I go back to the Inbox
    When I click compose message
    And I enter bot3 in the messageTo field
    Then I check if the privacy status is pep_yellow
    And I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pep_green
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |



	#Description: Make a handshake with a communication partner
	#Assumption: You didn’t exchange any message with user4 yet
	#Expectation: Privacy Status for the single existing message changes from “Secure…” to “Secure & Trusted”
  @QTR-17 @QTR-2
  Scenario Outline: Cucumber Handshake in existing message


    When I select account <account>
    When I click compose message
    And I enter bot6 in the messageTo field
    And I enter TM-17 in the messageSubject field
    And I enter TM-17 in the messageBody field
    Then I check if the privacy status is pEpRatingUnencrypted
    When I click the send message button
    And I wait for the new message
    And I click the last message received
    Then I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pep_green
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |



	#Description: Stop Trusting an identity
	#Assumption: Handshake has been done with at least 1 person (user4)
	#Expectation: After the cancellation of the handshake, the communication between Alice and me should be “Secure…”
  @QTR-18 @QTR-2 @QTR-169
  Scenario Outline: Cucumber Reset Handshake


    When I select account <account>
    When I click compose message
    And I send 1 message to bot4 with subject TM-18 and body cucumberStopTrusting
    Then I check the badge color of the first message is pEpRatingReliable
    When I click the last message received
    Then I check if the privacy status is pep_yellow
    When I click mistrust words
    Then I check if the privacy status is pep_red
    When I go back to the Inbox
    Then I check the badge color of the first message is pEpRatingMistrust
    And I click compose message
    And I send 1 message to bot4 with subject TM-18A and body cucumberStopTrustingMistrust
    Then I check the badge color of the first message is pEpRatingMistrust
    When I click the last message received
    Then I check if the privacy status is pep_red
    When I reset handshake
    And I go back to the Inbox
    And I click compose message
    And I send 1 message to bot4 with subject TM-18B and body cucumberStopTrustingReseted
    Then I check the badge color of the first message is pEpRatingReliable
    When I click the last message received
    Then I check if the privacy status is pep_yellow
    When I click confirm trust words
    And I go back to the Inbox
    Then I check the badge color of the first message is pEpRatingTrusted
    When I click compose message
    And I send 1 message to bot4 with subject TM-18C and body cucumberStopTrustingConfirmTrustWords
    Then I check the badge color of the first message is pEpRatingTrusted
    When I click the last message received
    Then I check if the privacy status is pep_green

    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |



	#Description: Assume the Trustwords between you and your communication partner don’t match and mistrust the communication partner
	# Assumption: You don’t have the public key of the communication partner user5
	# Expectation: The current message and new incoming messages will be red,
	#when I send a message to an untrusted address, the message has to be grey and unsecure.
  @QTR-19 @QTR-2
  Scenario Outline: Cucumber Handshake wrong trustwords


    When I select account <account>
    When I send 1 message to bot1 with subject TM-19 and body handshakeWrongTrustwords
    And I click the last message received
    And I click stop trusting words
    Then I check if the privacy status is pep_red
    When I go back to the Inbox
    And I send 1 message to bot1 with subject TM-19A and body handshakeWrongTrustwordsA
    Then I check the badge color of the first message is pEpRatingMistrust
    When I click the last message received
    Then I check if the privacy status is pep_red

    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |


  @QTR-550
  Scenario Outline: Cucumber Trust Reset: Mistrusted

    When I select account <account>
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeMistrusted
    And I click the last message received
    And I click stop trusting words
    Then I check if the privacy status is pep_red
    When I reset handshake
    Then I check if the privacy status is pep_yellow
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |


  @QTR-551
  Scenario Outline: Cucumber Trust Reset: Trusted
    When I select account <account>
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeTrusted
    And I click the last message received
    And I click confirm trust words
    Then I check if the privacy status is pep_green
    When I reset handshake
    Then I check if the privacy status is pep_yellow
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |


  @QTR-552
  Scenario Outline: Cucumber Trust Reset: Trusted message
    When I select account <account>
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeTrusted2
    And I click the last message received
    And I click confirm trust words
    Then I check if the privacy status is pep_green
    When I click reply message
    And I reset handshake
    Then I check if the privacy status is pep_no_color
    When I send 1 messages to bot2 with subject handshake2nd and body ThisWillBeGrey
    And I select the inbox
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    Examples:
      |account|
      | 0 |
      | 1 |
      | 2 |


  @QTR-553
  Scenario Outline: Cucumber Trust Reset: Mistrusted message
    When I select account <account>
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeMistrusted2
    And I click the last message received
    And I click stop trusting words
    Then I check if the privacy status is pep_red
    When I click reply message
    And I reset handshake
    Then I check if the privacy status is pep_no_color
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |





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
      |  1    |
      |  2    |



	#Second part of Attachment Send 1 File To 1 Contact (2/2)
  @QTR-127 @QTR-3
  Scenario Outline: Cucumber Attachment Send 1 File To 1 Contact (2/2)

    When I select account <account>
    And I click the last message received
    Then I open attached files
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |



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
      |  1    |
      |  2    |



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
      |  1    |
      |  2    |


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
      |  1    |
      |  2    |



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
      |  1    |
      |  2    |



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
      |  1    |
      |  2    |


  @QTR-417
  Scenario Outline: Cucumber Passive Mode ON

    When I go back to accounts list
    When I enable passive mode
    And I select account <account>
    And I click compose message
    And I send 1 messages to bot7 with subject passiveMode and body TestingPassiveMode
    And I click the first message
    Then I check if the privacy status is pep_no_color
    When I go back to the Inbox
    And I send 1 messages to bot7 with subject passiveModeEncrypted and body TestingPassiveModeEncrypted
    And I click the first message
    Then I check if the privacy status is pep_yellow
    When I go back to accounts list
    Then I disable passive mode
    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |


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
      |  1    |
      |  2    |




	#The automation should "visit" every page and verify some text or buttons on that page that should be there. Export settings, Import settings and verify that the settings are the same.

  @QTR-2053 @QTR-1
  Scenario Outline: Cucumber Export/Import settings

    When I select account <account>
    And I go back to accounts list
    And I change Global settings
    And I change Account settings
    And I export settings
    And I remove account 2
    And I remove account 1
    And I remove account 0
    And I import settings
    And I go back to accounts list
    Then I check Global settings
    And I check Account settings
    Examples:
      |account|
      |  0    |


  @QTR-1979
  Scenario Outline: Cucumber Widget

    When I select account <account>
    And I click compose message
    And I send 1 messages to bot1 with subject WidTest and body TestingWid_gets
    Then I test widgets

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
      | account |
      | 0       |
      | 1       |
      | 2       |

  @QTR-2311
  Scenario Outline: Cucumber Calendar Event2

    When I select account <account>
    And I click the last message received
    And I check that the Calendar is correct and body text is ThisIsTheBody
    Examples:
      | account |
      | 0       |
      | 1       |
      | 2       |


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
      |  1    |
      |  2    |


  @QTR-2319
  Scenario Outline: Cucumber Remove address clicking X button


    When I select account <account>
    And I click compose message
    And I enter myself in the messageTo field
    And I enter 3 unreliable recipients in the messageTo field
    Then I remove the 3 address clicking X button
    Then I remove the 2 address clicking X button
    Then I remove the 2 address clicking X button
    Then I check insecurity warnings are not there
    Then I check if the privacy status is pep_green

    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |
