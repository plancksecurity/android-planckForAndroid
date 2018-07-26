Feature: Sanity_1.2.3_MailFromNewContactEncrypted
  Mail from new contact: Check if public key is imported and my answer is yellow

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Sanity_1.2.3_MailFromNewContactEncrypted
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    And I click last message received
    Then I compare messageBody with body
    And I check if the privacy status is pep_yellow
    And I click view reply_message
    Then I fill messageSubject field with extraText
    And I fill messageBody field with bodyText
    And I check if the privacy status is pep_yellow
    Then I discard message
    And I remove account