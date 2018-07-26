Feature: Sanity_1.2.6_MailToMultipleContactsMixed
  Check Privacy Status when sending a message to multiple communication partners (mixed)

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Sanity_1.2.6_MailToMultipleContactsMixed
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click message compose
    And I send 1 message to bot2 with subject TestCase1.2.6 and body TestCase1.2.6
    And I click message compose
    Then I fill messageTo field with bot1
    And I fill messageTo field with bot2
    And I check if the privacy status is pep_yellow
    Then I fill messageTo field with unknown@user.es
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I check if the privacy status is pep_no_color
    And I fill messageSubject field with TestCase1.2.6
    And I fill messageBody field with TestCase1.2.6
    Then I fill messageTo field with empty
    Then I fill messageTo field with bot1
    And I fill messageTo field with bot2
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    And I check if the privacy status is pep_yellow
    Then I fill messageTo field with unknown@user.es
    And I fill messageSubject field with empty
    And I fill messageBody field with empty
    Then I check if the privacy status is pep_no_color
    Then I click send message button
    And I wait for new message
    And I go to sent folder
    And I click last message received
    Then I check if the privacy status is pep_no_color
    And I compare messageBody with TestCase1.2.6
    Then I remove account