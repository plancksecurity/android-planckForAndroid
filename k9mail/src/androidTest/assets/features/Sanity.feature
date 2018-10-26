Feature: Sanity

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test

  @login-scenarios
  Scenario: Test Sanity MailToNewContact
  Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
    when entering the email address of a new contact user1.
    Also verify if pEp attaches my public key to outgoing messages
  Assumption: No emails with the communication partner user1 have been exchanged so far
  Expectation: Privacy Status of outgoing message is “Unsecure”
    When I click message compose
    And I check if the privacy status is pEpRatingUndefined
    And I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with subjectText
    And I fill messageBody field with bodyText
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I click send message button
    And I go to sent folder
    And I click first message
    Then I compare messageBody with empty

  @login-scenarios
  Scenario: Test Sanity MailToSecondNewContact
  Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
    when entering the email address of the new contact user2.
    Also verify if pEp attaches my public key to outgoing messages
  Assumption: No emails with the communication partner user2 have been exchanged so far
  Expectation: Privacy Status of outgoing message is “Unsecure”
    When I click message compose
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I fill messageTo field with user2@mail.es
    And I fill messageSubject field with subject
    And I fill messageBody field with bodyText
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click send message button
    And I wait for new message

  @login-scenarios
  Scenario: Test Sanity MailFromNewContactEncrypted
  Description: You have a new communication partner using pEp.
    This communication partner sends you a message from pEp.
    Verify if the public key of the communication partner is imported and you can answer encrypted.
  Assumption: You didn’t receive any message from the communication partner so far
  Expectation: The public key of the communication partner is imported
    The answer to my communication partners message is encrypted
    When I click message compose
    And I send 1 message to bot1 with subject subject and body body
    And I click last message
    Then I compare messageBody with body
    And I check if the privacy status is pep_yellow
    When I click view reply_message
    And I fill messageSubject field with extraText
    And I fill messageBody field with bodyText
    Then I check if the privacy status is pep_yellow
    And I discard message

  @login-scenarios
  Scenario: SER-299 Ensure mails are encrypted when pEp says so
    When I click message
    And I click view reply_message
    Then I check if the privacy status is pep_yellow
    When I click send message button
    And I press back
    And I wait for new message
    And I click last message
    Then I compare messageBody with Rating/DecodedRating

  @login-scenarios
  Scenario: Test Sanity MailToExistingContactEncrypted
  Description: Test if pEp encrypts the message if you send an email to someone
    you already have the public key from, e.g. user1.
  Assumption: You already have a public key from your communication partner
  Expectation: The new message should be sent encrypted
    When I click message compose
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I click message compose
    And I fill messageTo field with bot1
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I check if the privacy status is pep_yellow
    When I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I go to sent folder
    And I click last message
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TestCase1.2.4

  @login-scenarios
  Scenario: Test Sanity MailToMultipleContactsEncrypted
  Description: Test if the Privacy Status of a message is correct,
    if it is sent to multiple people and the public key is available (user1 and user2)
  Assumption: You have public keys of (at least) 2 different communication partners.
  Expectation: The new message should be sent encrypted
    When I click message compose
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click last message
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click message compose
    And I send 1 message to bot2 with subject TestCase1.2.5 and body TestCase1.2.5
    And I click last message
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click message compose
    And I fill messageTo field with bot1
    And I fill messageTo field with bot2
    And I wait 5 seconds
    Then I check if the privacy status is pep_yellow
    When I fill messageSubject field with TestCase1.2.5
    And I fill messageBody field with TestCase1.2.5
    And I fill messageTo field with empty
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I fill messageTo field with bot2
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I check if the privacy status is pep_yellow
    When I click send message button
    And I wait for new message
    And I go to sent folder
    And I click last message
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with empty

  @login-scenarios
  Scenario: Test Sanity MailToMultipleContactsMixed
  Description:Test if the Privacy Status of a message is correct,
    if it is sent to multiple people. For one of the recipients, there is no public key available.
  Assumption: You have public keys of (at least) 2 different communication partners (user1 and user2).
    You don’t have the key of user3.
  Expectation: The message should be sent unencrypted
    When I click message compose
    And I send 1 message to bot1 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click message compose
    And I send 1 message to bot2 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click message compose
    And I fill messageTo field with bot1
    And I fill messageTo field with bot2
    Then I check if the privacy status is pep_yellow
    When I fill messageTo field with unknown@user.es
    Then I check if the privacy status is pep_no_color
    When I fill messageSubject field with TestCase1.2.6
    And I fill messageBody field with TestCase1.2.6
    And I fill messageTo field with empty
    And I fill messageTo field with bot1
    And I fill messageTo field with bot2
    Then I check if the privacy status is pep_yellow
    When I fill messageTo field with unknown@user.es
    Then I check if the privacy status is pep_no_color
    When I click send message button
    And I wait for new message
    And I go to sent folder
    And I click last message
    Then I check if the privacy status is pep_no_color
    And I compare messageBody with TestCase1.2.6

  @login-scenarios
  Scenario: Test Sanity DisableProtection
  Description: Send a message with disabled protection to a contact you have the public key (user1)
  Assumption: You have public keys of (at least) 1 communication partner
  Expectation: The message is sent unencrypted with no public key attached
    When I click message compose
    And I fill messageTo field with bot1
    And I select from message menu pep_force_unprotected
    And I fill messageSubject field with TestCase1.2.7
    And I fill messageBody field with TestCase1.2.7
    Then I check if the privacy status is pep_no_color
    When I click send message button
    And I wait for new message
    And I go to sent folder
    And I click last message
    Then I check if the privacy status is pep_no_color
    Then I remove account