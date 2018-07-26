Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test10 InboxActionBarChangingColorTest
    Then I click message compose
    And I fill messageTo field with myself
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I click send message button
    And I wait for new message
    And I click last message received
    Then I click view tvPep
    And I check status is pEpRatingTrusted
    Then I press back
    Then I check toolBar color is pep_green
    And I press back
    And I check toolBar color is pep_green
    Then I click message compose
    And I send 1 message to bot1 with subject subject and body body
    Then I click last message received
    Then I check toolBar color is pep_yellow
    And I press back
    And I check toolBar color is pep_green
    Then I remove account





