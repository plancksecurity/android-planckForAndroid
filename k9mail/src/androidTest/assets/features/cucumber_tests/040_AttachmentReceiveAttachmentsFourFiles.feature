Feature: Attachment Receive Attachments Three Files
  Background:




#Summary: This Cucumber test involves sending a message to a bot with multiple attachments, verifying the privacy status, waiting for the message to arrive, and opening the attachments.
#Description: This Cucumber test involves sending a message with multiple attachments and checking the privacy status of the message before and after sending it. The test begins with selecting an account and clicking the compose message button. Then, a message is sent to a bot with a subject and body, and another message is composed with attachments such as PDF, MSoffice, and a picture. The privacy status is checked before sending the message, and again after waiting for the message and clicking on it. Finally, the test opens the three attached files.
  Scenario: Cucumber Attachment Receive Attachments 4 Files

    When I click compose message
    And I send 1 message to bot5 with subject TM-131 and body attach3
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-131B in the messageSubject field
    And I enter attach3B in the messageBody field
    And I attach PDF
    And I attach MSoffice
    And I attach picture
    And I attach TXT
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted
    And I open 4 attached files


