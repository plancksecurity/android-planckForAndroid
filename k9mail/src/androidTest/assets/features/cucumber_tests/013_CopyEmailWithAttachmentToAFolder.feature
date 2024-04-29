Feature: Copy email with attachment to a folder
  Background:



    #Summary: This test composes and sends a message with attachments. It copies the message to the "spam" folder, verifies privacy status, compares message content and opens attachments.
  #Description: The test involves composing a message with attachments and sending it. The test then copies the message to the "spam" folder and checks the privacy status, compares the message content with expected values from a JSON file, and opens attachments.

  Scenario: Cucumber Copy email with attachment to a folder
    When I click compose message
    And I enter myself in the messageTo field
    And I enter copyThisMessageWithAttachments in the messageSubject field
    And I enter ThisMessageWithAttachmentWillBeCopiedToAnotherFolder in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    When I click the send message button
    And I wait for the message and click it
    And I copy the message to the folder spam
    Then I check the privacy status is Trusted
    And I compare messageBody with ThisMessageWithAttachmentWillBeCopiedToAnotherFolder
    And I open 3 attached files
    When I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    And I compare messageBody with ThisMessageWithAttachmentWillBeCopiedToAnotherFolder
    Then I check the privacy status is Trusted
    And I open 3 attached files
    And I press back
    And I go to inbox folder from navigation menu

