Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test10 InboxActionBarChangingColorTest
    Then I click message compose
    And I fill messageTo field with myself
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I click send message button
    And I wait for new message
    And I click last message
    Then I click view tvPep
    And I check in the handshake dialog if the privacy status is pEpRatingTrusted
    Then I check if the privacy status is planck_green
    And I press back
    And I check if the privacy status is planck_green
    Then I click message compose
    And I send 1 message to bot1 with subject subject and body body
    Then I click last message
    Then I check if the privacy status is planck_yellow
    And I press back
    And I check if the privacy status is planck_green
    Then I remove account





