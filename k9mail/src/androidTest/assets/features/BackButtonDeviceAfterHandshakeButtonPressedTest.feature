Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test3 backButtonDeviceAfterHandshakeButtonPressed
    When I send 3 messages to bot1 with subject subject and body body
    And I click last message
    Then I click message status
    And I confirm trust words
    And I remove account


