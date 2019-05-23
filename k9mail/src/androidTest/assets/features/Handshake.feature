Feature: Handshake

  Keys for these test users will be obtained from the test bot

  Background:
    Given I created an account
    Given I run the tests

  @TM-16 @TM-2
  Scenario: Test Handshake HandshakeInNewMessage
  Description: Make a handshake with a communication partner
  Assumption: You didn’t make a handshake with communication partner user1 yet
  Expectation: Privacy Status is “Secure & Trusted” for new messages with the trusted partner
  Scenario: Cucumber Handshake in new Message
    When I click compose message
    And I send 1 message to bot3 with subject subject and body body
    And I click the last message received
    And I go back to the Inbox
    When I click compose message
    And I enter bot3 in the messageTo field
    Then I check if the privacy status is pep_yellow
    And I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted

  @TM-17 @TM-2
  Scenario: Test Handshake HandshakeInExistingMessage
  Description: Make a handshake with a communication partner
  Assumption: You didn’t exchange any message with user4 yet
  Expectation: Privacy Status for the single existing message
  changes from “Secure…” to “Secure & Trusted”
  Scenario: Cucumber Handshake in existing message
    When I click compose message
    And I enter bot4 in the messageTo field
    And I enter TestCase1.2.10 in the messageSubject field
    And I enter TestCase1.2.10 in the messageBody field
    Then I check if the privacy status is pEpRatingUnencrypted
    When I click the send message button
    And I wait for the new message
    And I click the last message received
    Then I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted

  @TM-18 @TM-2
  Scenario: Test Handshake StopTrusting
  Description: Stop Trusting an identity
  Assumption: Handshake has been done with at least 1 person (user4)
  Expectation: After the cancellation of the handshake,
  the communication between Alice and me should be “Secure…”
  Scenario: Cucumber Stop trusting
    When I click compose message
    And I send 1 message to bot4 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pep_green
    When I stop trusting
    Then I check if the privacy status is pep_green
    When I go back to the Inbox
    And I click compose message
    And I send 1 message to bot4 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pep_yellow

  @TM-19 @TM-2
  Scenario: Test Handshake WrongTrustwords
  Description: Assume the Trustwords between you and your communication partner don’t match
  and mistrust the communication partner
  Assumption: You don’t have the public key of the communication partner user5
  Expectation: The current message and new messages will be red
  Scenario: Cucumber Handshake wrong trustwords
    When I send 1 message to bot1 with subject subject and body body
    And I click the last message received
    And I click wrong trust words
    Then I check if the privacy status is pEpRatingMistrust
    When I go back to the Inbox
    And I send 1 message to bot1 with subject subject and body body
    And I click the last message received
    Then I check if the privacy status is pEpRatingMistrust