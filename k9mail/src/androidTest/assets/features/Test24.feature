Feature: Handshake_1.2.9_HandshakeInNewMessage
  Disable Protection (public key attached)

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Handshake_1.2.9_HandshakeInNewMessage
    When I click message compose



    Then I send 1 message to bot1 with subject TestCase1.2.7 and body TestCase1.2.7
    And I click message compose
    Then I fill messageTo field with bot1
    And I select from message menu pep_force_unprotected
    And I fill messageSubject field with TestCase1.2.7
    And I fill messageBody field with TestCase1.2.7
    Then I check toolBar color is pep_no_color
    And I click send message button
    And I wait for new message
    And I go to sent folder
    And I click last message received
    Then I check toolBar color is pep_no_color
    Then I remove account