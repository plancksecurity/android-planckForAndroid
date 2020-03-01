Feature: Test
  Background:
    Given I created an account
    Given I run the tests

  @TM-OLD
  Scenario: Test Old Version
    When I click compose message
    And I send 1 message to bot1 with subject YellowColor and body YellowColorBody
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    When I go back to the Inbox
    When I click compose message
    And I send 1 message to bot2 with subject YellowToGreen and body YellowToGreenColorBody
    And I click compose message
    And I enter bot2 in the messageTo field
    And I enter GreenColor in the messageSubject field
    And I enter GreenColorBody in the messageBody field
    And I click confirm trust words
    And I click the send message button
    And I wait for the message and click it
    Then I check if the privacy status is pep_green
    When I go back to the Inbox
    When I click compose message
    And I send 1 message to bot3 with subject YellowToRed and body YellowToRedColorBody
    And I click compose message
    And I enter bot3 in the messageTo field
    And I enter RedColor in the messageSubject field
    And I enter RedColorBody in the messageBody field
    And I click wrong trust words
    And I click the send message button
    And I wait for the message and click it
    Then I check if the privacy status is pep_red

