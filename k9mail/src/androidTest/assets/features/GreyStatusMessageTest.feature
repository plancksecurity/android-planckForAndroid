Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account


  @login-scenarios
  Scenario: Test6 GreyStatusMessageTest
    When I click message compose
    Then I click view pEp_indicator
    And I check status is pEpRatingUndefined
    Then I press back
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I click view pEp_indicator
    And I check status is pEpRatingUnencrypted
    Then I press back
    Then I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I click view pEp_indicator
    And I check status is pEpRatingUndefined
    Then I press back
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I click view pEp_indicator
    And I check status is pEpRatingUnencrypted
    Then I press back
    Then I discard message
    And I remove account

