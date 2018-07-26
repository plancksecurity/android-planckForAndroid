Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test14 pEpStatusIncomingTrustedMessageShouldBeGreen
    Then I click message compose
    And I send 1 message to bot1 with subject subject and body body
    And I click last message received
    Then I click view tvPep
    And I click view confirmTrustWords
    Then I click view tvPep
    And I check status is pEpRatingTrusted
    Then I press back
    And I go back to message compose
    Then I click message compose
    And I fill messageTo field with bot1
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    And I click view pEp_indicator
    And I check color is pep_green at position 0
    Then I press back
    And I discard message
    Then I remove account






