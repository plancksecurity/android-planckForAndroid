Feature: Test
  Background:
    Given I created an account
  @TM-01
  Scenario Outline: Cucumber Click on Search button

    When I select account <account>
    Then I click compose message

    Examples:
      |account|
      |  0    |

  @TM-02
  Scenario Outline: Cucumber Click on Search

    When I select account <account>
    Then I click compose message

    Examples:
      |account|
      |  0    |
