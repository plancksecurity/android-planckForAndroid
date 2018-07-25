Feature: Attachment_1_2_15_SendEmbeddedPictureToMultipleContacts
  Send embedded picture to multiple contacts

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test Attachment_1_2_15_SendEmbeddedPictureToMultipleContacts
    When I click message compose
    Then I send 1 message to bot with subject TestCase1.2.15 and body TestCase1.2.15
    And I click message compose
    Then I send 1 message to secondBot with subject TestCase1.2.15 and body TestCase1.2.15
    And I click message compose
    Then I fill messageTo field with bot
    Then I fill messageTo field with secondBot
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I check toolBar color is pep_yellow



    And I attach PDF
    And I attach MSoffice
    And I attach picture
    And I attach settings
    And I click send message button
    And I wait for new message
    Then I go to sent folder
    And I click last message received
    And I remove account