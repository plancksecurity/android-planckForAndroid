Feature: Reset Own Key when Trusted Partner
  Background:
    Given I created an account

    #Summary: The scenario involves sending messages to a bot, confirming trust words, checking privacy status, resetting own key, and verifying privacy status after sending messages with different subjects and bodies. The desired outcome is to ensure that privacy status is "Trusted" for certain messages and "Encrypted" for others.
  #Description: This scenario tests the messaging application's functionality related to privacy and encryption. It includes actions such as sending messages to a bot, confirming trust words, checking and verifying privacy status, resetting the user's own key, and sending additional messages to verify the privacy status again. The purpose is to ensure that the privacy status is accurately reflected as "Trusted" or "Encrypted" for different scenarios and interactions within the application.
  Scenario: Cucumber Reset Own Key when Trusted Partner
    And I send 1 messages to bot2 with subject ResetKey and body ResetPartnersKey
    And I click the last message received
    And I compare rating_string from json file with unencrypted
    And I click confirm trust words
    Then I check if the privacy status is Encrypted
    And I go back to the Inbox
    And I reset own key
    When I click compose message
    And I enter bot2 in the messageTo field
    And I enter ResetKey2 in the messageSubject field
    And I enter NewMessageAfterReset in the messageBody field
    Then I check the privacy status is Trusted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Trusted
    And I compare rating_string from json file with reliable
    When I go back to the Inbox
    When I click compose message
    And I enter bot2 in the messageTo field
    And I enter key_reset_bot in the messageSubject field
    And I enter BotWillResetOwnKey in the messageBody field
    Then I check the privacy status is Trusted
    When I click the send message button
    And I send 2 messages to bot2 with subject AfterReset and body ResetPartnersKey
    And I click the first message
    Then I check the privacy status is Encrypted
    And I compare rating_string from json file with reliable


