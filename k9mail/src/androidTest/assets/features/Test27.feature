Feature: Handshake_1.2.12_WrongTrustwords
  Handshake: Wrong Trustwords

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Handshake_1.2.12_WrongTrustwords
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.12 and body TestCase1.2.12
    And I click last message received
    Then I click message status
    And I reject trust words
    Then I check status is pEpRatingMistrust
    Then I press back
    And I press back
    Then I click message compose
    And I fill messageTo field with bot1
    And I fill messageSubject field with subjectUnsecure
    And I fill messageBody field with bodyUntrusted
    Then I click send message button
    And I wait for new message
    And I click last message received
    Then I click view reply_message
    And I check toolBar color is pep_red
    And I remove account