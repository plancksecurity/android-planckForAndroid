Feature: Sanity_1.2.1_MailToNewContact
  Keys for these test users will be obtained from the test bot

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Sanity_1.2.1_MailToNewContact
    When I click message compose
    And I check status is pEpRatingUndefined
    Then I press back
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with subjectText
    And I fill messageBody field with bodyText
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    Then I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check in the handshake dialog if the privacy status is pEpRatingUndefined
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check in the handshake dialog if the privacy status is pEpRatingUnencrypted
    And I click send message button
    And I go to sent folder
    And I click first message
    Then I compare messageBody with bodyText
    And I remove account