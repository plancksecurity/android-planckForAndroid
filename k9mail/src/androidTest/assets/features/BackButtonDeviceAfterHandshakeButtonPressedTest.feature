Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test3 backButtonDeviceAfterHandshakeButtonPressed
    When I send 3 messages to bot
    And I click last message received
    Then I click message status
    And I confirm trust words
    And I remove account


