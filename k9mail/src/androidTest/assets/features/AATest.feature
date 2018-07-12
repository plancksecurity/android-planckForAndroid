Feature: Test
  This is the first cucumber test

  Background:
    Given I create an account

  @login-scenarios
  Scenario: Test19 1.2.4 GreyStatusMessageTest
    When I click message compose
    Then I check status is pEpRatingUndefined
