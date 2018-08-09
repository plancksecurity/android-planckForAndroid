Feature: Handshake_1.2.9_HandshakeInNewMessage
  Disable Protection (public key attached)

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Handshake_1.2.9_HandshakeInNewMessage
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.9 and body TestCase1.2.9
    And I click last message
    Then I save trustWords

    And I press back    //Debe existir esta línea de pressback? Será más difícil hacer un estándard con Outlook

    Then I click message compose
    And I fill messageTo field with bot1
    And I check if the privacy status is pep_yellow

  Click “Secure…”. You will see a new Window called “Privacy Status”.




    Then I remove account