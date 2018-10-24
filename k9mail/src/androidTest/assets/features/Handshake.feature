Feature: Handshake

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test

  @login-scenarios
  Scenario: Test Handshake HandshakeInNewMessage
  Description: Make a handshake with a communication partner
  Assumption: You didn’t make a handshake with communication partner user1 yet
  Expectation: Privacy Status is “Secure & Trusted” for new messages with the trusted partner
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    And I click last message
    And I check if the privacy status is pep_yellow
    And I confirm trust words match
    Then I click confirm trust words
    And I check if the privacy status is pEpRatingTrusted

  @login-scenarios
  Scenario: Test Handshake HandshakeInExistingMessage
  Description: Make a handshake with a communication partner
  Assumption: You didn’t exchange any message with user4 yet
  Expectation: Privacy Status for the single existing message
    changes from “Secure…” to “Secure & Trusted”
    Given I click message compose
    Then I fill messageTo field with bot2
    And I fill messageSubject field with TestCase1.2.10
    And I fill messageBody field with TestCase1.2.10
    Then I check if the privacy status is pEpRatingUnencrypted
    Then I click send message button
    And I click last message
    And I click view reply_message
    And I confirm trust words match
    And I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted

  @login-scenarios
  Scenario: Test Handshake StopTrusting
  Description: Stop Trusting an identity
  Assumption: Handshake has been done with at least 1 person (user4)
  Expectation: After the cancellation of the handshake,
    the communication between Alice and me should be “Secure…”
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    And I click last message
    Then I stop trusting
    And I check if the privacy status is pep_green
    Then I go back to message compose
    And I click message compose
    And I send 1 message to bot1 with subject subject and body body
    And I click last message
    Then I check if the privacy status is pep_yellow

  @login-scenarios
  Scenario: Test Handshake WrongTrustwords
  Description: Assume the Trustwords between you and your communication partner don’t match
    and mistrust the communication partner
  Assumption: You don’t have the public key of the communication partner user5
  Expectation: The current message and new messages will be red
    Then I send 1 message to bot3 with subject subject and body body
    And I click last message
    Then I click wrong trust words
    Then I check if the privacy status is pEpRatingMistrust
    Then I go back to message compose
    Then I send 1 message to bot3 with subject subject and body body
    And I click last message
    Then I check if the privacy status is pEpRatingMistrust
