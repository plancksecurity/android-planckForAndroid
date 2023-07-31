Feature: Trust Reset: Mistrusted
  Background:
    Given I created an account


#Summary: the user sends a message with specific content to a bot. They then stop trusting the message, leading to a privacy status of "Dangerous." After resetting the partner key, the privacy status is restored to "Encrypted."
  #Description: the user engages with a messaging system involving a bot. They send a message with specific content, but due to mistrusted words, the privacy status becomes "Dangerous." To rectify this, the user resets the partner key, which restores the privacy status to "Encrypted." The scenario serves to test the system's response to mistrusted messages and the successful recovery of privacy status after a key reset.
  Scenario: Cucumber Trust Reset: Mistrusted

    And I send 1 messages to bot2 with subject handshake and body ThisWillBeMistrusted
    And I click the last message received
    And I click stop trusting words
    Then I check if the privacy status is Dangerous
    When I reset partner key
    Then I check if the privacy status is Encrypted



