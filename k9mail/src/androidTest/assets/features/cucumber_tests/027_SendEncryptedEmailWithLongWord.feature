Feature: Send Encrypted email with long word
  Background:




#Summary: This Cucumber test is testing the functionality of sending and receiving encrypted messages with a bot. The test includes composing and sending encrypted messages, verifying the privacy status of messages, and comparing message content.
#Description: This Cucumber test is a series of steps that test the functionality of sending and receiving encrypted messages. The test involves selecting an account, composing and sending a message to a bot, checking the privacy status, and comparing the message body with a specific string. The test also includes actions such as saving a draft message, discarding a message, and verifying the privacy status of sent messages. Overall, the test aims to ensure that the messaging system is functioning correctly and that messages are being sent and received securely.
  Scenario: Cucumber Send Encrypted email with long word

    When I click compose message
    And I send 1 message to bot1 with subject sendEncrypted and body sendEncryptedTest
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter EFA-1976 in the messageSubject field
    And I enter longWord in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with longWord
    Then I check the privacy status is Encrypted
    When I go back to the Inbox
    And I go to sent folder from navigation menu
    And I click the first message
    Then I compare messageBody with longWord



