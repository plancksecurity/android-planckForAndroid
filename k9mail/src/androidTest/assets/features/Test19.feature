Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test19 1.2.3 GreyStatusMessageTest
    When I click message compose
    Then I send 1 message to bot with subject subject and body body
    And I click last message received
    Then I compare messageBody with body
    And I check toolBar color is pep_yellow
    And I click view reply_message
    Then I fill messageSubject field with extraText
    And I fill messageBody field with bodyText
    And I check toolBar color is pep_yellow
    Then I discard message
    And I remove account