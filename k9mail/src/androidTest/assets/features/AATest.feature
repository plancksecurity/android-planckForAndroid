Feature: Test
  This is the first cucumber test

  Background:
    Given I create an account

  @login-scenarios
  Scenario: Test16 YellowStatusEmailFromBot
    When I click message compose
    Then I send 1 message to bot
    And I click last message received
    Then I click view reply_message
    And I click view pEp_indicator
    And I check toolBar color is pep_yellow
    And I press back
    Then I discard message
    And I go back to message compose
    And I click message compose
    Then I fill messageTo field with bot
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    And I fill again messageTo field with unknown@user.is
    Then I click view pEp_indicator
    And I check status color is pep_yellow at position 0
    And I check status color is pep_no_color at position 1
    Then I press back
    And I discard message
    And I remove account