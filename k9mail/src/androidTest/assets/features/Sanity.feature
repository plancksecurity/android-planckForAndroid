Feature: Sanity

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test

  @login-scenarios
  Scenario: Test Sanity_1.2.1_MailToNewContact
    When I click message compose
    And I check if the privacy status is pEpRatingUndefined
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with subjectText
    And I fill messageBody field with bodyText
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    Then I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click send message button
    And I go to sent folder
    And I click first message
    Then I compare messageBody with empty

  @login-scenarios
  Scenario: Test Sanity_1.2.2_MailToSecondNewContact
    When I click message compose
    And I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I fill messageTo field with user2@mail.es
    And I fill messageSubject field with subject
    And I fill messageBody field with bodyText
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click send message button
    And I wait for new message

  @login-scenarios
  Scenario: Test Sanity_1.2.3_MailFromNewContactEncrypted
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    And I click last message
    Then I compare messageBody with body
    And I check if the privacy status is pep_yellow
    And I click view reply_message
    Then I fill messageSubject field with extraText
    And I fill messageBody field with bodyText
    And I check if the privacy status is pep_yellow
    Then I discard message

  @login-scenarios
  Scenario: SER-299 Ensure mails are encrypted when pEp says so
    When I click message
    Then I click view reply_message
    And I check if the privacy status is pep_yellow
    Then I click send message button
    And I press back
    And I wait for new message
    And I click last message
    Then I compare messageBody with Rating/DecodedRating

  @login-scenarios
  Scenario: Test Sanity_1.2.4_MailToExistingContactEncrypted
    When I click message compose
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I click message compose
    Then I fill messageTo field with bot1
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check if the privacy status is pep_yellow
    Then I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    Then I go to sent folder
    And I click last message
    And I check if the privacy status is pep_yellow
    And I compare messageBody with TestCase1.2.4

  @login-scenarios
  Scenario: Test Sanity_1.2.5_MailToMultipleContactsEncrypted
    When I click message compose
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I send 1 message to bot1 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click last message
    And I check if the privacy status is pep_yellow
    Then I press back
    And I click message compose
    And I send 1 message to bot2 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click last message
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
    And I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I fill messageTo field with bot2
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check if the privacy status is pep_yellow
    Then I click send message button
    And I wait for new message
    Then I go to sent folder
    And I click last message
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with empty

  @login-scenarios
  Scenario: Test Sanity_1.2.6_MailToMultipleContactsMixed
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click message compose
    And I send 1 message to bot2 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click message compose
    Then I fill messageTo field with bot1
    And I fill messageTo field with bot2
    And I check if the privacy status is pep_yellow
    Then I fill messageTo field with unknown@user.es
    Then I check if the privacy status is pep_no_color
    And I fill messageSubject field with TestCase1.2.6
    And I fill messageBody field with TestCase1.2.6
    Then I fill messageTo field with empty
    Then I fill messageTo field with bot1
    And I fill messageTo field with bot2
    And I check if the privacy status is pep_yellow
    Then I fill messageTo field with unknown@user.es
    Then I check if the privacy status is pep_no_color
    Then I click send message button
    And I wait for new message
    And I go to sent folder
    And I click last message
    Then I check if the privacy status is pep_no_color
    And I compare messageBody with TestCase1.2.6

  @login-scenarios
  Scenario: Test Sanity_1.2.7_DisableProtection
    When I click message compose
    Then I fill messageTo field with bot1
    And I select from message menu pep_force_unprotected
    And I fill messageSubject field with TestCase1.2.7
    And I fill messageBody field with TestCase1.2.7
    Then I check if the privacy status is pep_no_color
    And I click send message button
    And I wait for new message
    And I go to sent folder
    And I click last message
    Then I check if the privacy status is pep_no_color
    Then I remove account