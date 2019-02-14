Feature: Attachment

  Keys for these test users will be obtained from the test bot

  Background:
    Given I created an account
    Given I run the tests


  @scenario
  Scenario: Test Attachment Send1FileTo1Contact (1/2)
  Description: Send 1 attachment to 1 contact
  Assumption: I already have the public key of the other contact
  and can send the message encrypted(user1)
  Expectation: Then message is in SENT items and I can open the attachments
    When I click compose message
    And I send 1 message to bot1 with subject subject and body body
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter subject in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    Then I check if the privacy status is pep_yellow
    When I attach MSoffice
    And I click the send message button
    And I wait for the new message

  @scenario
  Scenario: Test Attachment Send1FileTo1Contact (2/2)
  Description: Send 1 attachment to 1 contact
  Assumption: I already have the public key of the other contact
  and can send the message encrypted(user1)
  Expectation: Then message is in SENT items and I can open the attachments
    And I click the last message received
    Then I open attached files

  @scenario
  Scenario: Test Attachment Send4FilesTo3Contacts (1/2)
  Description: Send 4 attachment to 3 contacts
  Assumption: I already have the public key of 3 contacts and can send the message encrypted
  Expectation: Then message is in SENT items and I can open the attachments
  Steps for Testing
  •	Create a new message
  •	Enter “To…” (3 recipients you have a public key from, e.g. user1, user2 and user3), any subject and any body.
  •	Verify that the Privacy Status changes to “Secure…” or “Secure & Trusted”
  •	Attach 4 files with 4 different formats (1 picture, 1 MS Office document, 1PDF, 1 other file) to the email. Make sure the total size is between 5 and 15MB.
  •	Send the message
  •	VERIFY if the message is in SENT items of your mailbox and you can open the attachment.
  •	Ask the recipient, if he could open all attachments
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


  @scenario
  Scenario: Test Attachment Send4FilesTo3Contacts (2/2)
  Description: Send 4 attachment to 3 contacts
  Assumption: I already have the public key of 3 contacts and can send the message encrypted
  Expectation: Then message is in SENT items and I can open the attachments
  Steps for Testing
  •	Create a new message
  •	Enter “To…” (3 recipients you have a public key from, e.g. user1, user2 and user3), any subject and any body.
  •	Verify that the Privacy Status changes to “Secure…” or “Secure & Trusted”
  •	Attach 4 files with 4 different formats (1 picture, 1 MS Office document, 1PDF, 1 other file) to the email. Make sure the total size is between 5 and 15MB.
  •	Send the message
  •	VERIFY if the message is in SENT items of your mailbox and you can open the attachment.
  •	Ask the recipient, if he could open all attachments
    And I go to the sent folder
    And I click the last message received
    Then I open attached files

  @OutlookTest
  Scenario: Test Attachment SendEmbeddedPictureToMultipleContacts
  Description: Send message with an embedded picture to multiple contacts
  Assumptions: I already have the public key of 2 contacts and can send the message encrypted
  Expectation: Then message is in SENT items and I can read it including embedded picture
  Steps for Testing
  •	Create a new message
  •	Enter “To…” (2 recipients you have a public key from, e.g. user1 and user2), any subject.
  •	Ensure that the Privacy Status changes to “Secure…” or “Secure & Trusted”
  •	In the body, first enter some text, then embed a picture (Insert-> Picture. Not as attachment). Also enter some text below the embedded picture. It could look like this.

  •	Send the message
  •	VERIFY if the message is in SENT items of your mailbox and you can read the preview.
  •	Open the message (double click)
  •	VERIFY if you can read the message including the embedded picture.

  @scenario
  Scenario: Test Attachment ReceiveAttachmentsOneFile
  Description: Receive one attachment that has only been sent to me from another pEp user
  Assumptions: The communication partner has my private key
  and can send me encrypted messages with pEp.
  Expectation: I can read the email and
  Steps for Testing
  •	Ask a communication partner to send a message with 1 attachment (Privacy Status: “Secure…” or “Secure & Trusted”).
  •	When the message arrives, click on it.
  •	Make sure, the message has been sent encrypted (Privacy Status “Secure…” or “Secure & Trusted”)
  •	VERIFY if you can read the content of the message
  •	Open the attachment
  •	VERIFY if the attachment opens as expected
    When I click compose message
    And I enter bot1 in the messageTo field
    And I enter subject in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    And I attach PDF
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    Then I compare messageBody from attachment with ThisIsTheBody
    And I open attached files


  @scenario
  Scenario: Test Attachment ReceiveAttachmentsThreeFiles
  Description: Receive three attachments that have been sent to me
  and another person from another pEp user
  Assumptions: The communication partner has my private key
  and can send me encrypted messages with pEp
  Expectation: I can read the email and attachment
  Steps for Testing
  •	Ask a communication partner to send a message with 3 attachments encrypted to myself and another person
  •	When the message arrives, click on it.
  •	Make sure, the message has been sent encrypted (Privacy Status “Secure…” or “Secure & Trusted”)
  •	VERIFY if you can read the content of the message
  •	Open the attachment
  •	VERIFY if the attachment opens as expected
  •	Repeat 5 and 6 for each attachment
    When I click compose message
    And I send 1 message to bot1 with subject subject and body body
    And I send 1 message to bot2 with subject subject and body body
    And I click compose message
    And I enter bot1 in the messageTo field
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

  @OutlookTest
  Scenario: Test Attachment ReceiveMessageWithEmbeddedPicture
  Description: Receive an encrypted message with an embedded picture
  Assumptions: The communication partner has my private key
    and can send me encrypted messages with pEp
  Expectation: I can read the email and including the embedded picture
  Steps for Testing
  •	Ask a communication partner to send a message with an embedded picture
  •	When the message arrives, click on it.
  •	Make sure, the message has been sent encrypted (Privacy Status “Secure…” or “Secure & Trusted”)
  •	VERIFY if you can read the content of the message in the preview of Outlook. In the current version, the embedded picture will be attach to the message (and not embedded)
  •	Open the message
  •	VERIFY if the message opens and the body including the embedded picture are displayed correctly.
