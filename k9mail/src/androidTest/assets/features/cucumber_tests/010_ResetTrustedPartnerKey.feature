Feature: Reset Trusted Partner Key
  Background:



#Summary: The scenario involves sending a message to a bot with specific subject and body, confirming trust words, checking privacy status, resetting the partner key, composing another message, and verifying privacy status. The desired outcome is to ensure that the privacy status is "Trusted" for the initial message, "NotEncrypted" after doing the Key Reset, and "Encrypted" after sending and receiving the second message.
#Description: This scenario tests the messaging application's functionality related to privacy and encryption. It includes actions such as sending messages to a bot, confirming trust words, checking and verifying privacy status, resetting the partner key, composing another message, and checking the privacy status again. The purpose is to ensure that the privacy status is accurately reflected as "Trusted" for the initial message, "NotEncrypted" for the subsequent message, and "Encrypted" after sending and receiving the second message.
  Scenario: Cucumber Reset Trusted Partner Key
    And I send 1 messages to bot2 with subject ResetKey and body ResetPartnersKey
    And I click the last message received
    And I click confirm trust words
    Then I check if the privacy status is Encrypted
    Then I reset partner key
    When I press back
    When I click compose message
    And I enter bot2 in the messageTo field
    And I enter ResetKey2 in the messageSubject field
    And I enter NewMessageAfterReset in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted

