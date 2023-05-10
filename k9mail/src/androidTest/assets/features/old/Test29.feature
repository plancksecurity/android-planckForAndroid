Feature: Attachment_1.2.14_Send4FilesTo3Contacts
  Send 4 files to 3 contacts

    Background:

  @login-scenarios
  Scenario: Test Attachment_1.2.14_Send4FilesTo3Contacts (1/2)
    Given I create an account
    When I click message compose
    Then I send 1 message to bot1 with subject TestCase1.2.14 and body TestCase1.2.14
    And I click message compose
    Then I send 1 message to bot2 with subject TestCase1.2.14 and body TestCase1.2.14
    And I click message compose
    Then I send 1 message to bot3 with subject TestCase1.2.14 and body TestCase1.2.14
    And I click message compose
    Then I fill messageTo field with bot1
    Then I fill messageTo field with bot2
    Then I fill messageTo field with bot3
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I check if the privacy status is planck_yellow
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    And I attach settings
    And I click send message button
    And I wait for new message

  @login-scenarios
  Scenario: Test Attachment_1.2.14_Send4FilesTo3Contacts (2/2)
    Then I go to sent folder
    And I click last message
    And I open attached files
    And I remove account