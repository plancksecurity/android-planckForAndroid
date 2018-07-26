Feature: Attachment_1.2.13_Send1FileTo1Contact
  Send 1 file to 1 contact

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Attachment_1.2.13_Send1FileTo1Contact
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.13 and body TestCase1.2.13
    And I click message compose
    Then I fill messageTo field with bot1
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I check toolBar color is pep_yellow
    Then Set external mock settings
    Then I attach files to message
    And I click send message button
    Then I go to sent folder
    And I click last message received
    And I open attached file
    And I remove account