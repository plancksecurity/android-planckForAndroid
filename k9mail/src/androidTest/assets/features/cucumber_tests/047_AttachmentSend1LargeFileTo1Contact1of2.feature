Feature: Attachment Send 1 Large File To 1 Contact Part1
  Background:


    #Summary: send a large file as an attachment to a contact, confirming successful delivery.
    # Description: checks the system's ability to send a large file as an attachment to a specific contact. It involves composing a message, attaching a large file, and confirming successful message delivery.

  Scenario: Cucumber Attachment Send 1 Large File To 1 Contact Part1

    #And I remove all messages
    And I click compose message
    And I send 1 message to bot5 with subject LargeAttachment and body LargeAttachment
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter LargeAttachment2 in the messageSubject field
    And I enter attachALargeFile in the messageBody field
    When I attach largeFile
    And I click the send message button
    And I wait for the new message



