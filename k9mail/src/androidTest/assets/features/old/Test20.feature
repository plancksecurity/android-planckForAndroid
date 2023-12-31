Feature: Sanity_1.2.4_MailToExistingContactEncrypted
  New Mail to existing communication partner should be encrypted

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Sanity_1.2.4_MailToExistingContactEncrypted
    When I click message compose
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I click message compose
    Then I fill messageTo field with bot1
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check if the privacy status is planck_yellow
    Then I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    Then I go to sent folder
    And I click last message
    And I check if the privacy status is planck_yellow
    And I compare messageBody with TestCase1.2.4
    Then I remove account


