Feature: Reset Handshake
  Background:
    Given I created an account


#Summary: the user sends multiple messages to a bot with different contents. They check and manipulate the privacy status by mistrusting, resetting partner key, and confirming trust words. The process includes verifying badge colors for each message to indicate whether it's "Encrypted," "Dangerous," or "Trusted."
  #Description: the user engages in a series of actions to interact with a messaging system involving a bot. They send multiple messages with various content and subject lines. Throughout the scenario, the user checks and modifies the privacy status of these messages, using actions like mistrusting, resetting the partner key, and confirming trust words. The privacy status is indicated by badge colors, which can be "Encrypted," "Dangerous," or "Trusted" for different messages. The scenario serves as a comprehensive test of the messaging system's privacy and trust features.
  Scenario: Cucumber Reset Handshake
    When I click compose message
    And I send 1 message to bot4 with subject TM-18 and body cucumberStopTrusting
    Then I check the badge color of the first message is Encrypted
    When I click the last message received
    Then I check if the privacy status is Encrypted
    When I click mistrust words
    Then I check if the privacy status is Encrypted
    When I go back to the Inbox
    Then I check the badge color of the first message is Encrypted
    And I click compose message
    And I enter bot4 in the messageTo field
    And I enter TM-18A in the messageSubject field
    And I enter cucumberStopTrustingMistrust in the messageBody field
    When I click the send message button
    And I go to suspicious folder from navigation menu
    Then I check the badge color of the first message is Dangerous
    When I click the last message received
    Then I check if the privacy status is Dangerous
    When I reset partner key
    And I go back to the Inbox
    And I press back
    And I click compose message
    And I send 1 message to bot4 with subject TM-18B and body cucumberStopTrustingReset
    Then I check the badge color of the first message is Encrypted
    When I click the last message received
    Then I check if the privacy status is Encrypted
    When I click confirm trust words
    And I go back to the Inbox
    Then I check the badge color of the first message is Encrypted
    When I click compose message
    And I send 1 message to bot4 with subject TM-18C and body cucumberStopTrustingConfirmTrustWords
    Then I check the badge color of the first message is Trusted
    When I click the last message received
    Then I check if the privacy status is Trusted


