Feature: Test
  Background:
    Given I created an account
    Given I run the tests


  @TM-NEW
  Scenario: Test NEW Version
    When I click compose message
    When I go back to the Inbox
    And I click message at position 1
    Then I check if the privacy status is pep_red
    When I go back to the Inbox
    And I click message at position 3
    Then I check if the privacy status is pep_green
    When I go back to the Inbox
    And I click message at position 5
    Then I check if the privacy status is pep_yellow
    When I go back to the Inbox
    When I click compose message
    And I send 1 message to bot1 with subject YellowColor and body YellowColorBody
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I go back to the Inbox
    When I click compose message
    And I send 1 message to bot2 with subject Green and body GreenColorBody
    And I click the last message received
    Then I check if the privacy status is pep_green
    When I go back to the Inbox
    When I click compose message
    And I send 1 message to bot3 with subject Red and body RedColorBody
    And I click the last message received
    Then I check if the privacy status is pep_red