Feature: Test
  Background:
    Given I created an account
  @TM-01
  Scenario Outline: Cucumber Click on Search button

    When I select account <account>
    Then I remove account 0

    Examples:
      |account|
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
      |  0    |
