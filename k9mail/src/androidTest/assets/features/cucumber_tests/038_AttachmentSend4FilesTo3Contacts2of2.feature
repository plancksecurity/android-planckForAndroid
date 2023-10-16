Feature: Attachment Send 4 Files To 3 Contacts Part2
  Background:



#Summary: This Cucumber test is the second part of the test and involves selecting an account, navigating to the "sent" folder, clicking on the last message received, and then opening four attached files.
#Description: This Cucumber test is focused on verifying the ability to access and open attached files in a received email. The test starts by selecting the specified account and navigating to the sent folder. The last received message is then clicked, and the test verifies that 4 attached files can be opened. The purpose of this test is to ensure that the email service is functioning correctly and that users can access and view attachments sent to them.
  Scenario: Cucumber Attachment Send 4 Files To 3 Contacts Part2

    And I go to sent folder from navigation menu
    And I click the last message received
    Then I open 4 attached files



