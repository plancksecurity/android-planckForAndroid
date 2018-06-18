Feature: Test
  This is the first cucumber test

  Background:
    Given I create an account


  @login-scenarios
  Scenario: Test1 sendMessageToYourselfWithDisabledProtectionAndCheckReceivedMessageIsUnsecure
    When I click message compose
    Then I fill messageTo field with self
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I click view pEp_indicator
    And I check status is pEpRatingTrusted
    Then I press back
    And I select from message menu pep_force_unprotected
    Then I click view pEp_indicator
    And I check status is pEpRatingUnencrypted
    Then I press back
    Then I click send message button
    Then I wait for new message
    And I go to sent folder
    And I click first message
    And I click view tvPep
    And I check status is pEpRatingTrusted
    Then I press back
    And I remove account

