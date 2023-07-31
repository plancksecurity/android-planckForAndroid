Feature: Move Non encrypted email with attachment to a folder
  Background:



    #Summary: The user sends a non-encrypted email with attachments to a bot with the subject "Reset" and a specific message body. After sending, they check the privacy status, move the email to the "spam" folder, and later verify the privacy status again. Finally, they open and review the attached files before returning to the Inbox folder.
  #Description: The user interacts with a system or application, likely using Cucumber for behavior-driven testing. They initiate a process by sending a non-encrypted email with attachments to a bot and specific content. The user then performs actions like checking the privacy status, moving the email to the "spam" folder, and verifying the status again. Additionally, they access and review the attached files before navigating back to the Inbox folder. The scenario aims to test the system's functionality related to handling emails, attachments, and privacy settings.
  Scenario: Cucumber Move Non encrypted email with attachment to a folder
    When I send 1 message to bot1 with subject Reset and body ThisIsTheFirstMessage
    When I click compose message
    And I enter bot1 in the messageTo field
    And I enter key_reset_bot in the messageSubject field
    And I enter EncryptedMessageWithAttachmentMovedToAnotherFolder in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is NotEncrypted
    When I move the message to the folder spam
    And I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I check the privacy status is NotEncrypted
    And I open 3 attached files
    And I press back
    And I go to inbox folder from navigation menu


