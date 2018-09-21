Feature: Attachment

  Keys for these test users will be obtained from the test bot

  Background:
    Given I create an account
    Given I start test

  @login-scenarios
  Scenario: Test Attachment_1.2.13_Send1FileTo1Contact
    Given I set timeoutTest to 600 seconds
    When I click message compose
    Then I send 1 message to bot1 with subject subject and body body
    And I click last message
    Then I check if the privacy status is pep_yellow
    Then I attach MSoffice
    And I click send message button
    Then I wait for new message
    And I click last message
    Then I open 1 attached files


  @login-scenarios
  Scenario: Test Attachment_1.2.14_Send4FilesTo3Contacts (1/2)
    When I click message compose
    Then I fill messageTo field with myself
    And I fill messageSubject field with subject
    And I fill messageBody field with body
    Then I check if the privacy status is pep_green
    And I attach picture
    And I attach MSoffice
    And I attach PDF
    And I attach picture2
    And I attach MSoffice2
    And I attach PDF2
    And I click send message button

  @login-scenarios
  Scenario: Test Attachment_1.2.14_Send4FilesTo3Contacts (2/2)
    Then I go to sent folder
    And I click last message
    And I open 6 attached files

  @login-scenarios
  Scenario: Test Attachment_1_2_15_SendEmbeddedPictureToMultipleContacts
    Given I click message compose
    Then I fill messageTo field with bot1
    And I fill messageTo field with bot2


  @login-scenarios
  Scenario: Test Attachment_1.2.16_ReceiveAttachmentsOneFile

  @login-scenarios
  Scenario: Test Attachment_1.2.17_ReceiveAttachmentsThreeFiles

  @login-scenarios
  Scenario: Test Attachment_1.2.18_ReceiveMessageWithEmbeddedPicture
