Feature: Handshake_1.2.11_StopTrusting
  Stop Trusting (Revoke Trust / Cancel Handshake)

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Handshake_1.2.11_StopTrusting
    When I click message compose
    Then I send 1 message to bot with subject TestCase1.2.5 and body TestCase1.2.5
    And I click last message received
    Then I click message status
    And I confirm trust words
    Then I click message status
    And I untrust trust words
    And I press back
    Then I check toolBar color is pep_yellow
    Then I click view reply_message
    And I click send message button
    And I wait for new message
    And I click last message received
    And I check toolBar color is pep_yellow
    Then I remove account