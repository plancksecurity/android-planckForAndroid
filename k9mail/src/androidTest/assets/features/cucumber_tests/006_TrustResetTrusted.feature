Feature: Trust Reset: Trusted
  Background:
    Given I created an account

#Summary: the user sends a message with specific content to a bot and confirms trust. The privacy status becomes "Trusted." After resetting the partner key, the privacy status is restored to "Encrypted."
  #Description: the user interacts with a messaging system involving a bot. They send a message with specific content, and upon confirming trust in the message, the privacy status is updated to "Trusted." However, the user later resets the partner key, and as a result, the privacy status is reverted to "Encrypted." The scenario aims to test the system's response to trusted messages and the successful recovery of privacy status after a key reset.

  Scenario: Cucumber Trust Reset and Trusted
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeTrusted
    And I click the last message received
    And I click confirm trust words
    Then I check if the privacy status is Trusted
    When I reset partner key
    Then I check if the privacy status is Trusted
    And I press back
    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot2 in the messageTo field
    Then I check the privacy status is NotEncrypted
    And I discard the message


