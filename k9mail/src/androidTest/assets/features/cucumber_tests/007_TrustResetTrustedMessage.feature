Feature: Trust Reset: Trusted message
  Background:
    Given I created an account


    #Summary:
  #Description:

  Scenario: Cucumber Trust Reset: Trusted message
    And I send 1 messages to bot2 with subject handshake and body ThisWillBeTrusted2
    And I click the last message received
    And I click confirm trust words
    Then I check if the privacy status is Trusted
    When I click reply message
    And I reset partner key
    Then I check if the privacy status is NotEncrypted
    When I send 1 messages to bot2 with subject handshake2nd and body ThisWillBeGrey
    And I select the inbox
    And I click the last message received
    Then I check if the privacy status is Encrypted


