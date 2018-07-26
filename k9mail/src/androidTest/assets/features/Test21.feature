Feature: Sanity_1.2.5_MailToMultipleContactsEncrypted
  Check Privacy Status when sending a message
  to multiple communication partners (public key available)

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Sanity_1.2.5_MailToMultipleContactsEncrypted
    When I click message compose
    Then I check status is pEpRatingUndefined
    And I press back
    Then I send 1 message to bot1 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click last message received
    And I check if the privacy status is pep_yellow
    Then I press back
    And I click message compose
    And I send 1 message to bot2 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click last message received
    And I check if the privacy status is pep_yellow
    Then I press back
    And I click message compose
    Then I fill messageTo field with bot1
    And I fill messageTo field with bot2
    Then I wait 5 seconds
    And I check if the privacy status is pep_yellow
    And I fill messageSubject field with TestCase1.2.5
    And I fill messageBody field with TestCase1.2.5
    And I fill messageTo field with empty
    And I check status is pEpRatingUndefined
    And I press back
    Then I fill messageTo field with bot2
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check if the privacy status is pep_yellow
    Then I click send message button
    And I wait for new message
    Then I go to sent folder
    And I click last message received
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TestCase1.2.5
    Then I remove account