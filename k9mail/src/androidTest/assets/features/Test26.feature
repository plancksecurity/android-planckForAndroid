Feature: Handshake_1.2.11_StopTrusting
  Stop Trusting (Revoke Trust / Cancel Handshake)

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Handshake_1.2.11_StopTrusting
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.11 and body TestCase1.2.11
    And I click last message
    And I confirm trust words
    And I stop trusting
    And I press back
    Then I check if the privacy status is pep_yellow
    Then I click view reply_message
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    And I click send message button
    Then I press back
    And I wait for new message
    And I click last message
    And I check if the privacy status is pep_yellow
    Then I remove account