Feature: Trust Reset: Trusted message
  Background:
    Given I created an account


    #Summary: trust and privacy settings during message interactions.
  #Description: verify the behavior of a message in response to trust and privacy settings. It covers sending messages, confirming trust, resetting keys, and checking privacy status, ensuring that the system behaves as expected when trust levels change during message interactions.

  Scenario: Cucumber Trust Reset and Trusted message
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeTrusted2
    And I click the last message received
    And I click confirm trust words
    Then I check if the privacy status is Encrypted
    And I reset partner key
    When I click reply message
    Then I check if the privacy status is NotEncrypted
    And I discard the message
    And I go back to the Inbox
    When I send 1 messages to bot2 with subject handshake2nd and body ThisWillBeGrey
    And I click the last message received
    Then I check if the privacy status is Encrypted


