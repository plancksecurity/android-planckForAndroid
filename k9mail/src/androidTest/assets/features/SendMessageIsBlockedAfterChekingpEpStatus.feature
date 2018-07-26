Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test13 SendMessageIsBlockedAfterChekingpEpStatus
    When I click message compose
    Then I fill messageTo field with myself
    And I fill messageSubject field with subject
    And I fill messageBody field with thisIsTheBody
    Then I click view pEp_indicator
    And I check status is pEpRatingTrusted
    Then I press back
    And I select from message menu pep_force_unprotected
    Then I click view pEp_indicator
    And I check status is pEpRatingUnencrypted
    Then I press back
    And I click send message button
    Then I wait for new message
    Then I click message compose
    And I fill messageTo field with myself
    And I fill messageSubject field with subject
    And I fill messageBody field with thisIsTheBody
    Then I click send message button
    And I wait for new message
    Then I remove account





