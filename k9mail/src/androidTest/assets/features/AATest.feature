Feature: Test
  Background:
    Given I created an account
   @QA-sync
  Scenario Outline: Cucumber KeySync

    When I select account <account>
     And I keysync devices A and B
     And I keysync device C
     And I setup second account for devices A and B
     Then I remove from keysync device C
    Examples:
      |account|
      |  0    |
