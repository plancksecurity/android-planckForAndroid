Feature: Attachment Send 1 Large File To 1 Contact (1/2)
  Background:


  Scenario: Cucumber Attachment Send 1 Large File To 1 Contact (1/2)

    And I remove all messages
    And I click compose message
    And I send 1 message to bot5 with subject LargeAttachment and body LargeAttachment
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter LargeAttachment2 in the messageSubject field
    And I enter attachALargeFile in the messageBody field
    When I attach largeFile
    And I click the send message button
    And I wait for the new message



