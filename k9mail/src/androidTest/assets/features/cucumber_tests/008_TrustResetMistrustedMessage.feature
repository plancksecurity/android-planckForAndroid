Feature: Trust Reset: Mistrusted message
  Background:
    Given I created an account

        #Summary:
  #Description:

  Scenario: Cucumber Trust Reset: Mistrusted message
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeMistrusted2
    And I click the last message received
    And I click stop trusting words
    Then I check if the privacy status is Dangerous
    When I click reply message
    And I reset partner key
    Then I check if the privacy status is NotEncrypted



