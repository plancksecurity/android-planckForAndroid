Feature: Handshake_1.2.10_HandshakeInExistingMessage
   Handshake in existing message

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Handshake_1.2.10_HandshakeInExistingMessage
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.10 and body TestCase1.2.10
    And I click last message
    Then I save trustWords
    And I compare messageBody with TestCase1.2.10

  In the reply of the bot, click “Secure…”. You will see a new Window called “Privacy Status”.

    Then I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click send message button
    Then I wait for new message
    And I click last message
    And I save trustWords
    And I compare messageBody with TestCase1.2.10


    Then I remove account