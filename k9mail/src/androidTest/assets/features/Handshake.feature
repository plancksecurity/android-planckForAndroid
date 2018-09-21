Feature: Handshake

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test

  @login-scenarios
  Scenario: Test Handshake_1.2.9_HandshakeInNewMessage
    Given I set timeoutTest to 600 seconds
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    And I click message compose
    Then I fill messageTo field with bot1
    And I check if the privacy status is pep_yellow
    And I confirm trust words match
    Then I click confirm trust words
    And I check if the privacy status is pEpRatingTrusted

  @login-scenarios
  Scenario: Test Handshake_1.2.10_HandshakeInExistingMessage
    Given I click message compose
    Then I fill messageTo field with bot1
    And I fill messageSubject field with TestCase1.2.10
    And I fill messageBody field with TestCase1.2.10
    Then I check if the privacy status is pEpRatingUndefined
    Then I click send message button
    And I click last message
    And I click view reply_message
    Then I confirm trust words match
    And I click confirm trust words
    Then I check if the privacy status is pEpRatingTrusted

  @login-scenarios
  Scenario: Test Handshake_1.2.11_StopTrusting
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    And I click last message
    Then I stop trusting
    And I check if the privacy status is pep_yellow
    Then I click view reply_message
    And I send 1 message to bot1 with subject subject and body body
    And I click last message
    Then I check if the privacy status is pep_yellow




  @login-scenarios
  Scenario: Test Handshake_1.2.12_WrongTrustwords
    Then I send 1 message to bot2 with subject subject and body body
    And I click last message
    Then I click view reply_message
