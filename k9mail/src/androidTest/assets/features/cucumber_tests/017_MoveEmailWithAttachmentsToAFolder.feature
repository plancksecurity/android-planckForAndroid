Feature: Move email with attachments to a folder
  Background:
    Given I created an account



    #Summary: This test composes and sends a message with attachments, moves it to another folder, and: verifies the message content, checks privacy status and opens attachments.
  #Description: This test scenario involves composing and sending a message with attachments, moving the message to the "spam" folder, and performing various actions on the message. It includes entering the sender's own email address, subject, and message content. Attachments such as a PDF, MS Office file, and a picture are added to the message. After sending the message, the test verifies that it has been received and clicks on it. The message is then moved to the "spam" folder, and the test navigates back to the inbox. Next, the test selects the "spam" folder and clicks on the first message within it. It compares the message body with an expected value stored in a JSON file and checks the privacy status. The test proceeds to open three attached files and then returns to the previous screen. Finally, the test navigates back to the inbox folder from the navigation menu. These steps outline the overall flow of the Cucumber test scenario.

  Scenario: Cucumber Move email with attachments to a folder
    When I click compose message
    And I enter myself in the messageTo field
    And I enter moveThisMessageWithAttachments in the messageSubject field
    And I enter ThisMessageWithAttachmentWillBeMovedToAnotherFolder in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    When I click the send message button
    And I wait for the message and click it
    And I move the message to the folder spam
    And I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I compare messageBody with ThisMessageWithAttachmentWillBeMovedToAnotherFolder
    And I check the privacy status is Trusted
    And I open 3 attached files
    And I press back
    And I go to inbox folder from navigation menu



