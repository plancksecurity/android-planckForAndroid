Feature: Sanity_1.2.2_MailToSecondNewContact
  Mail to second new contact

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Sanity_1.2.2_MailToSecondNewContact
    When I click message compose
    And I check status is pEpRatingUndefined
    Then I press back
    Then I fill messageTo field with user2@mail.es
    And I fill messageSubject field with subject
    And I fill messageBody field with bodyText
    And I check status is pEpRatingUnencrypted
    Then I press back
    And I click send message button
    And I wait for new message
    And I remove account

