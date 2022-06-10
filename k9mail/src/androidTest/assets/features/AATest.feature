Feature: Test
  Background:
    Given I created an account

  @QTR-412
  Scenario Outline: Cucumber Unsecure Warning Test


    When I select account <account>

    And I click compose message
    And I enter 5 recipients in the messageTo field

    #And I enter bot1 in the messageTo field
    Then I check is unsecure

    Examples:
      |account|
      |  0    |
      |  1    |
      |  2    |
