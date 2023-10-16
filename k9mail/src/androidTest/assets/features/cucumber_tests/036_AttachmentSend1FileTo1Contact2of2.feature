Feature: Attachment Send 1 File To 1 Contact Part2
  Background:




#Summary: This Cucumber test is the second part of the test and involves selecting an account and opening the last received message and opening one of the attached files in it.
#Description: This Cucumber test scenario involves accessing a previously received message and opening an attached file. The test assumes that an email has been received and the user is currently viewing the inbox or message list. The test involves selecting the last message received and opening an attached file. The attached file may be of any type, such as a PDF or an image file. The purpose of this test is to ensure that the user can access and view attachments that are sent with emails.
  Scenario: Cucumber Attachment Send 1 File To 1 Contact Part2

    And I click the last message received
    Then I open 1 attached files


