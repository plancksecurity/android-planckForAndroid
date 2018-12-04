Feature: Handshake

  Keys for these test users will be obtained from the test bot

  Background:
    Given I created an account
    Given I run the tests

  @login-scenarios
  Scenario: Test Handshake HandshakeInNewMessage
  Description: Make a handshake with a communication partner
  Assumption: You didn’t make a handshake with communication partner user1 yet
  Expectation: Privacy Status is “Secure & Trusted” for new messages with the trusted partner
    When I click compose message
    And I send 1 message to bot1 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pep_yellow
    And I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted
    And I go back to message compose

  @login-scenarios
  Scenario: Test Handshake HandshakeInExistingMessage
  Description: Make a handshake with a communication partner
  Assumption: You didn’t exchange any message with user4 yet
  Expectation: Privacy Status for the single existing message
  changes from “Secure…” to “Secure & Trusted”
    When I click compose message
    And I enter bot2 in the messageTo field
    And I enter TestCase1.2.10 in the messageSubject field
    And I enter TestCase1.2.10 in the messageBody field
    Then I check if the privacy status is pEpRatingUnencrypted
    When I click the send message button
    And I click the last message
    And I click reply message
    Then I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted
    And I discard the message
    And I go back to message compose

  @login-scenarios
  Scenario: Test Handshake StopTrusting
  Description: Stop Trusting an identity
  Assumption: Handshake has been done with at least 1 person (user4)
  Expectation: After the cancellation of the handshake,
  the communication between Alice and me should be “Secure…”
    When I send 1 message to bot2 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pep_green
    When I stop trusting
    Then I check if the privacy status is pep_green
    When I go back to message compose
    And I click compose message
    And I send 1 message to bot2 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pep_yellow

  @login-scenarios
  Scenario: Test Handshake WrongTrustwords
  Description: Assume the Trustwords between you and your communication partner don’t match
  and mistrust the communication partner
  Assumption: You don’t have the public key of the communication partner user5
  Expectation: The current message and new messages will be red
    When I send 1 message to bot3 with subject subject and body body
    And I click the last message received
    And I click wrong trust words
    Then I check if the privacy status is pEpRatingMistrust
    When I go back to message compose
    And I send 1 message to bot3 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pEpRatingMistrust