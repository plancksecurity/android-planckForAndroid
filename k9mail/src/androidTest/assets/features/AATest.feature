Feature: Test
  Background:
    Given I created an account



  @QTR-1979
  Scenario Outline: Cucumber Widget

    When I select account <account>
    And I click compose message
    And I send 1 messages to bot1 with subject WidTest and body TestingWid_gets
    Then I test widgets

    Examples:
      |account|
      |  0    |

