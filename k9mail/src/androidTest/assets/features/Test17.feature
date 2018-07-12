Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test17 1.2.1 GreyStatusMessageTest
    When I click message compose
    And I check status is pEpRatingUndefined
    Then I press back
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with subjectText
    And I fill messageBody field with bodyText
    And I check status is pEpRatingUnencrypted
    Then I press back
    Then I fill messageTo field with empty
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check status is pEpRatingUndefined
    Then I press back
    Then I fill messageTo field with unknownuser@mail.es
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check status is pEpRatingUnencrypted
    Then I press back
    And I click send message button
    And I go to sent folder
    And I click first message
    Then I compare messageBody with bodyText
    And I remove account