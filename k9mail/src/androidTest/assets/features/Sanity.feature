Feature: Sanity

  Keys for these test users will be obtained from the test bot

  Background:
    Given I created an account
    Given I run the tests

  @TM-6
  Scenario: Test Sanity MailToNewContact
  Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
  when entering the email address of a new contact user1.
  Also verify if pEp attaches my public key to outgoing messages
  Assumption: No emails with the communication partner have been exchanged so far
  Expectation: Privacy Status of outgoing message is “Unsecure”
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter unknownuser@mail.es in the messageTo field
    And I enter subjectText in the messageSubject field
    And I enter bodyText in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter empty in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter unknownuser@mail.es in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    When I enter longText in the messageBody field
    And I click the send message button
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText

  @TM-A1
  Scenario: Test Send Unencrypted email with long text
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from attachment with longText
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText

  @TM-A2
  Scenario: Test Send Encrypted email with long text
    When I click compose message
    And I send 1 message to bot1 with subject sendEncrypted and body sendEncryptedTest
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter subject in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from attachment with longText
    Then I check if the privacy status is pep_yellow
    When I go back to message compose
    And I go to the sent folder
    And I click the first message
    Then I compare messageBody with longText

  @TM-A3
  Scenario: Test Send Unencrypted email with long text
    When I click compose message
    And I check if the privacy status is pEpRatingUndefined
    And I enter bot2 in the messageTo field
    And I enter longText in the messageBody field
    And I go back and save as draft
    And I go to the drafts folder
    And I click the first message
    Then I compare messageBody with longText

  @TM-7
  Scenario: Test Sanity MailToSecondNewContact
  Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
  when entering the email address of the new contact user2.
  Also verify if pEp attaches my public key to outgoing messages
  Assumption: No emails with the communication partner user2 have been exchanged so far
  Expectation: Privacy Status of outgoing message is “Unsecure”
    When I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter user2@gmail.es in the messageTo field
    And I enter subject in the messageSubject field
    And I enter bodyText in the messageBody field
    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click the send message button
    And I wait for the new message

  @TM-8
  Scenario: Test Sanity MailFromNewContactEncrypted
  Description: You have a new communication partner using pEp.
  This communication partner sends you a message from pEp.
  Verify if the public key of the communication partner is imported and you can answer encrypted.
  Assumption: You didn’t receive any message from the communication partner so far
  Expectation: The public key of the communication partner is imported
  The answer to my communication partners message is encrypted
    When I click compose message
    And I send 1 message to bot1 with subject subject and body body
    And I click the last message received
    Then I compare messageBody from attachment with body
    And I check if the privacy status is pep_yellow
    When I click reply message
    And I enter extraText in the messageSubject field
    And I enter bodyText in the messageBody field
    Then I check if the privacy status is pep_yellow
    And I click the send message button
    And I go back to message compose
    And I wait for the new message
    #@Juan: We should send the message and check if the answer of the bot, if the message we sent was encrypted

  @TM-9
  Scenario: SER-299 Ensure mails are encrypted when pEp says so
    When I click the last message received
    And I click reply message
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I press back
    And I wait for the new message
    And I wait for the message and click it
    Then I compare rating from attachment with 6
    And I compare rating_string from attachment with PEP_rating_reliable

  @TM-10
  Scenario: Test Sanity MailToExistingContactEncrypted
  Description: Test if pEp encrypts the message if you send an email to someone
  you already have the public key from, e.g. user1.
  Assumption: You already have a public key from your communication partner
  Expectation: The new message should be sent encrypted
    When I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.4 and body TestCase1.2.4
    And I go to the sent folder
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with TestCase1.2.4

  @TM-11
  Scenario: Test Sanity MailToMultipleContactsEncrypted
  Description: Test if the Privacy Status of a message is correct,
  if it is sent to multiple people and the public key is available (user1 and user2)
  Assumption: You have public keys of (at least) 2 different communication partners.
  Expectation: The new message should be sent encrypted
    When I click compose message
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I send 1 message to bot1 with subject TestCase1.2.5 and body TestCase1.2.5
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I send 1 message to bot2 with subject TestCase1.2.5 and body TestCase1.2.5
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    When I press back
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    And I wait 5 seconds
    Then I check if the privacy status is pep_yellow
    When I enter TestCase1.2.5 in the messageSubject field
    And I enter TestCase1.2.5 in the messageBody field
    And I enter empty in the messageTo field
    Then I check in the handshake dialog if the privacy status is pEpRatingUndefined
    When I enter bot2 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check if the privacy status is pep_yellow
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I wait for the message and click it
    Then I check if the privacy status is pep_yellow
    And I compare messageBody with empty
    And I go back to message compose

  @TM-12
  Scenario: Test Sanity MailToMultipleContactsMixed
  Description:Test if the Privacy Status of a message is correct,
  if it is sent to multiple people. For one of the recipients, there is no public key available.
  Assumption: You have public keys of (at least) 2 different communication partners (user1 and user2).
  You don’t have the key of user3.
  Expectation: The message should be sent unencrypted
    When I click compose message
    And I send 1 message to bot1 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click compose message
    And I send 1 message to bot2 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter unknown@user.es in the messageTo field
    Then I check if the privacy status is pep_no_color
    When I enter TestCase1.2.6 in the messageSubject field
    And I enter TestCase1.2.6 in the messageBody field
    And I enter empty in the messageTo field
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check if the privacy status is pep_yellow
    When I enter unknown@user.es in the messageTo field
    Then I check if the privacy status is pep_no_color
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I wait for the message and click it
    Then I check if the privacy status is pep_no_color
    And I compare messageBody with TestCase1.2.6

  @TM-13
  Scenario: Test Sanity DisableProtection
  Description: Send a message with disabled protection to a contact you have the public key (user1)
  Assumption: You have public keys of (at least) 1 communication partner
  Expectation: The message is sent unencrypted with no public key attached
    When I click compose message
    And I enter bot1 in the messageTo field
    And I select from message menu pep_force_unprotected
    And I enter TestCase1.2.7 in the messageSubject field
    And I enter TestCase1.2.7 in the messageBody field
    Then I check if the privacy status is pep_no_color
    When I click the send message button
    And I wait for the new message
    And I go to the sent folder
    And I wait for the message and click it
    Then I check if the privacy status is pep_no_color
    Then I remove account