Feature: Trust Reset: Mistrusted message
  Background:
    Given I created an account

        #Summary: mistrusted messages and trust reset during interactions.
  #Description: examines how a messaging system handles mistrusted messages and trust reset. It includes sending a mistrusted message, marking it as dangerous, resetting the partner key, and checking the resulting privacy status during a reply.

  Scenario: Cucumber Trust Reset and Mistrusted message
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeMistrusted2
    And I click the last message received
    And I click mistrust words
    Then I check if the privacy status is Encrypted
    And I reset partner key
    When I click reply message
    Then I check if the privacy status is NotEncrypted



