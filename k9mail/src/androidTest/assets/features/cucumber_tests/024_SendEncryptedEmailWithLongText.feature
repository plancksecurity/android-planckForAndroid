Feature: Send Encrypted email with long text
  Background:



#Summary: This Cucumber test involves sending an encrypted message to bot1 and verifying that the message is received with the correct privacy status and content. It also checks that the sent message is stored correctly in the sent folder.
#Description: This Cucumber test is designed to test the messaging functionality of an email service. The test involves sending encrypted messages to a bot and verifying that the privacy status of the messages is Encrypted. There are also tests to verify that messages can be saved as drafts, and that the privacy status of drafts and sent messages is Encrypted. The test involves using various features of the email service, such as composing a message, sending a message, saving a draft, and navigating through different folders in the email service. The test also involves comparing message content with expected values stored in a JSON file.
  Scenario: Cucumber Send Encrypted email with long text

    When I click compose message
    And I send 1 message to bot1 with subject sendEncrypted and body sendEncryptedTest
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter TM153 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    Then I check the privacy status is Encrypted
    When I go back to the Inbox
    And I go to sent folder from navigation menu
    And I click the first message
    Then I compare messageBody with longText



