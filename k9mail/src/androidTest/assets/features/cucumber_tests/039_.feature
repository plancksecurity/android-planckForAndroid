Feature: Attachment Receive Attachments One File
  Background:


#Summary: This Cucumber test involves sending an email with an attachment, verifying the privacy status, and checking the contents of the email and attachment.
#Description: This Cucumber test involves sending an email with an attachment and verifying that the privacy status of the message is Encrypted. The test begins with selecting an account and composing a message. The user sends a message to a bot, attaches a PDF file, and verifies the privacy status of the message. The user then sends a second message, attaches another file, and verifies the privacy status of the message. Finally, the user compares the message body of the second message with a pre-defined JSON file and opens the attached file.
  Scenario: Cucumber Attachment Receive Attachments One File

    When I click compose message
    And I send 1 message to bot5 with subject EFA-130 and body beforeAttachment
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-130 in the messageSubject field
    And I enter attach1File in the messageBody field
    And I attach PDF
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted
    And I compare messageBody from json file with attach1File
    And I open 1 attached files



