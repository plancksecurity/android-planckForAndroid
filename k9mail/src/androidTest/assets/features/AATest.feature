Feature: Sanity

  Keys for these test users will be obtained from the test bot

  Background:
    Given I created an account
    Given I run the tests


	#Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
	# when entering the email address of a new contact bot1.
	# Also verify if pEp attaches my public key to outgoing messages
	# Assumption: No emails with the communication partner have been exchanged so far
	# Expectation: Privacy Status of outgoing message is “Unsecure”
  @TM-6 @TM-1
  Scenario: Cucumber Mail to new contact

    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot1 in the messageTo field
    And I enter subjectText in the messageSubject field
    And I enter bodyText in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter empty in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter bot1 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter longText in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText


  @TM-152 @TM-1
  Scenario: Cucumber Send Unencrypted email with long text
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

	#The long text should contain at least 15k characters.
  @TM-153 @TM-1
  Scenario: Cucumber Send Encrypted email with long text
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

	#[^long.txt]
	#is the link to long text
  @TM-154 @TM-1
  Scenario: Cucumber Save Draft email with long text
    When I send 1 message to bot2 with subject TestCase and body TestCase
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longText in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click the first message
    And I enter TM154B in the messageSubject field
    And I enter longText in the messageBody field
    And I save as draft
    And I wait for the new message
    And I click message at position 2
    Then I compare messageBody with longText

	#Scenario: Test Sanity MailFromNewContactEncrypted
	#Description: You have a new communication partner using pEp.
	#This communication partner sends you a message from pEp.
	#Verify if the public key of the communication partner is imported and you can answer encrypted.
	#Assumption: You didn’t receive any message from the communication partner so far
	#Expectation: The public key of the communication partner is imported
	#The answer to my communication partners message is encrypted
  @TM-8 @TM-1 @TM-169
  Scenario: Cucumber Mail from new contact encrypted

    When I click compose message
    And I send 1 message to bot1 with subject subject and body body
    And I click the last message received
    Then I compare messageBody from json file with body
    And I check if the privacy status is pep_yellow
    When I click reply message
    And I enter extraText in the messageSubject field
    And I enter bodyText in the messageBody field
    Then I check if the privacy status is pep_yellow
    And I click the send message button
    And I go back to the Inbox
    And I wait for the new message
		    #@Juan: We should send the message and check if the answer of the bot, if the message we sent was encrypted


	#Ensure mails are encrypted when pEp says so (SER-299)
  @TM-9 @TM-1
  Scenario: Cucumber: Ensure Mails are encrypted when pEp says so

    When I click the last message received
    And I click reply message
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I press back
    And I wait for the message and click it
    And I compare rating_string from json file with PEP_rating_reliable

	#Description: Test if pEp encrypts the message if you send an email to someone
	#you already have the public key from, e.g. user1.
	#Assumption: You already have a public key from your communication partner
	#Expectation: The new message should be sent encrypted
  @TM-10 @TM-1
  Scenario: Cucumber Mail to existing contact is encrypted

    When I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TestCase1.2.4

	#Description: Test if the Privacy Status of a message is correct,
	#if it is sent to multiple people and the public key is available (user1 and user2)
	#Assumption: You have public keys of (at least) 2 different communication partners.
	#Expectation: The new message should be sent encrypted
  @TM-11 @TM-1
  Scenario: Cucumber Mail to multiple contacts encrypted

    When I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I send 1 message to bot2 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter TestCase1.2.5 in the messageSubject field
    And I enter TestCase1.2.5 in the messageBody field
    And I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter bot2 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check if the privacy status is pep_yellow
    And I enter subject in the messageSubject field
    And I enter messageBody in the messageBody field
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with empty
    And I go back to the Inbox

	#Description:Test if the Privacy Status of a message is correct,
	#if it is sent to multiple people. For one of the recipients, there is no public key available.
	#Assumption: You have public keys of (at least) 2 different communication partners (user1 and user2).
	#You don’t have the key of user3.
	#Expectation: The message should be sent unencrypted
  @TM-12 @TM-1
  Scenario: Cucumber Mail to multiple contacts (mixed)

    When I click compose message
    And I send 1 message to bot1 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click compose message
    And I send 1 message to bot2 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter bot5 in the messageTo field
    Then I check if the privacy status is pEpRatingUnencrypted
    When I enter TestCase1.2.6 in the messageSubject field
    And I enter TestCase1.2.6 in the messageBody field
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
    And I compare messageBody with TestCase1.2.6

	#Description: Send a message with disabled protection to a contact you have the public key (user1)
	#Assumption: You have public keys of (at least) 1 communication partner
	#Expectation: The message is sent unencrypted with no public key attached
	#
  @TM-13 @TM-1
  Scenario: Cucumber Disable Protection

    When I click compose message
    And I enter bot1 in the messageTo field
    And I select from message menu pep_force_unprotected
    And I enter TestCase1.2.7 in the messageSubject field
    And I enter TestCase1.2.7 in the messageBody field
    Then I check if the privacy status is pep_no_color
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I click the last message received
    Then I check if the privacy status is pep_no_color

	#Description: Make a handshake with a communication partner
	#Assumption: You didn’t make a handshake with communication partner user1 yet
	#Expectation: Privacy Status is “Secure & Trusted” for new messages with the trusted partner
  @TM-16 @TM-2
  Scenario: Cucumber Handshake in new Message
    When I click compose message
    And I send 1 message to bot3 with subject subject and body body
    And I click the last message received
    And I go back to the Inbox
    When I click compose message
    And I enter bot3 in the messageTo field
    Then I check if the privacy status is pep_yellow
    And I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted

	#Description: Make a handshake with a communication partner
	#Assumption: You didn’t exchange any message with user4 yet
	#Expectation: Privacy Status for the single existing message changes from “Secure…” to “Secure & Trusted”
  @TM-17 @TM-2
  Scenario: Cucumber Handshake in existing message
    When I click compose message
    And I enter bot6 in the messageTo field
    And I enter TestCase1.2.10 in the messageSubject field
    And I enter TestCase1.2.10 in the messageBody field
    Then I check if the privacy status is pEpRatingUnencrypted
    When I click the send message button
    And I wait for the new message
    And I click the last message received
    Then I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted

	#Description: Stop Trusting an identity
	#Assumption: Handshake has been done with at least 1 person (user4)
	#Expectation: After the cancellation of the handshake, the communication between Alice and me should be “Secure…”
  @TM-18 @TM-2 @TM-169
  Scenario: Cucumber Stop trusting
    When I click compose message
    And I send 1 message to bot4 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I stop trusting
    Then I check if the privacy status is pep_red
    When I go back to the Inbox
    And I click compose message
    And I send 1 message to bot4 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pep_red

	#Description: Assume the Trustwords between you and your communication partner don’t match and mistrust the communication partner
	#Assumption: You don’t have the public key of the communication partner user5
	#Expectation: The current message and new incoming messages will be red
  @TM-19 @TM-2
  Scenario: Cucumber Handshake wrong trustwords
    When I send 1 message to bot1 with subject subject and body body
    And I click the last message received
    And I click wrong trust words
    Then I check if the privacy status is pEpRatingMistrust
    When I go back to the Inbox
    And I send 1 message to bot1 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pEpRatingMistrust

	#Description: Send 1 attachment to 1 contact
	# Assumption: I already have the public key of the other contact
	# and can send the message encrypted(user1)
	# Expectation: Then message is in SENT items and I can open the attachments
  @TM-126 @TM-3
  Scenario: Cucumber Attachment Send 1 File To 1 Contact (1/2)
    When I click compose message
    And I send 1 message to bot5 with subject subject and body body
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter subject in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    Then I check if the privacy status is pep_yellow
    When I attach MSoffice
    And I click the send message button
    And I wait for the new message

	#Second part of Attachment Send 1 File To 1 Contact (2/2)
  @TM-127 @TM-3
  Scenario: Cucumber Attachment Send 1 File To 1 Contact (2/2)
    And I click the last message received
    Then I open attached files

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
  @TM-128 @TM-3
  Scenario: Cucumber Attachment Send 4 Files To 3 Contacts (1/2)
    When I click compose message
    And I enter myself in the messageTo field
    And I enter subject in the messageSubject field
    And I enter body in the messageBody field
    Then I check if the privacy status is pep_green
    When I attach PDF
    And I attach MSoffice
    And I attach settings
    And I attach picture
    And I click the send message button
    And I wait for the new message

	#Second part of Attachment Send 4 Files To 3 Contacts (2/2)
  @TM-129 @TM-3
  Scenario: Cucumber Attachment Send 4 Files To 3 Contacts (2/2)
    And I go to the sent folder
    And I click the last message received
    Then I open attached files

	#  Description: Receive one attachment that has only been sent to me from another pEp user  Description: Receive one attachment that has only been sent to me from another pEp user  Assumptions: The communication partner has my private key  and can send me encrypted messages with pEp.  Expectation: I can read the email and  Steps for Testing  • Ask a communication partner to send a message with 1 attachment (Privacy Status: “Secure…” or “Secure & Trusted”).  • When the message arrives, click on it.  • Make sure, the message has been sent encrypted (Privacy Status “Secure…” or “Secure & Trusted”)  • VERIFY if you can read the content of the message  • Open the attachment  • VERIFY if the attachment opens as expected
  @TM-130 @TM-3
  Scenario: Cucumber Attachment Receive Attachments One File
    When I click compose message
    And I enter bot5 in the messageTo field
    And I enter subject in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    And I attach PDF
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    Then I compare messageBody from json file with ThisIsTheBody
    And I open attached files

	#Description: Receive three attachments that have been sent to me
	# and another person from another pEp user
	# Assumptions: The communication partner has my private key
	# and can send me encrypted messages with pEp
	# Expectation: I can read the email and attachment
	# Steps for Testing
	# • Ask a communication partner to send a message with 3 attachments encrypted to myself and another person
	# • When the message arrives, click on it.
	# • Make sure, the message has been sent encrypted (Privacy Status “Secure…” or “Secure & Trusted”)
	# • VERIFY if you can read the content of the message
	# • Open the attachment
	# • VERIFY if the attachment opens as expected
	# • Repeat 5 and 6 for each attachment
  @TM-131 @TM-3
  Scenario: Cucumber Attachment Receive Attachments Three Files
    When I click compose message
    And I send 1 message to bot5 with subject subject and body body
    And I send 1 message to bot2 with subject subject and body body
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter bot2 in the messageTo field
    And I enter subject in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    And I open attached files

  @TM-132 @TM-5
  Scenario: Cucumber Attachment Receive Attachments Three Files
    And I send 1 message to bot5 with subject 3messages and body body
    And I send 2 message to bot2 with subject 3messages and body body
    And I send 1 message to bot5 with subject 1message and body body
    And I send 1 message to bot2 with subject subject and body body
    Then I search for 0 messages with subject 0messages
    Then I search for 1 message with subject 1message
    Then I search for 3 messages with subject 3messages