Feature: Sanity_1.2.2_MailToSecondNewContact
  Mail to second new contact

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Sanity_1.2.2_MailToSecondNewContact
    When I click message compose
    And I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I fill messageTo field with user2@mail.es
    And I fill messageSubject field with subject
    And I fill messageBody field with bodyText
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click send message button
    And I remove account

