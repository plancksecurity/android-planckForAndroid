Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test18 1.2.2 GreyStatusMessageTest
    When I click message compose
    And I check status is pEpRatingUndefined
    Then I press back
    Then I fill messageTo field with user2@mail.es
    And I fill messageSubject field with subject
    And I fill messageBody field with bodyText
    And I check status is pEpRatingUnencrypted
    Then I press back
    And I click send message button
    And I remove account

