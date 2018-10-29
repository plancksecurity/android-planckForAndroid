Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test11 MessageUnsecureWhenDisableProtectionTest
    Then I click message compose
    And I fill messageTo field with myself
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I click view pEp_indicator
    And I check in the handshake dialog if the privacy status is pEpRatingTrusted
    Then I select from message menu pep_force_unprotected
    Then I click view pEp_indicator
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click send message button
    And I wait for new message
    And I click last message
    Then I click view tvPep
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    Then I remove account



