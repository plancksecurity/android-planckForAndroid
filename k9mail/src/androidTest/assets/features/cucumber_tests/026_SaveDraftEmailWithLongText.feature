Feature: Save Draft email with long text
  Background:



#Summary: This Cucumber test case describes a scenario in which a user saves a draft message, goes to the drafts folder to verify it, and then discards it.
#Description: This Cucumber test involves a series of actions related to composing, sending, and saving draft messages in an email application. The test is focused on verifying the functionality related to the privacy status and content of the messages.
#
#The first step is selecting an email account, followed by sending a message to a specific bot with a given subject and body. The next step is to compose another message to the same bot, with a different subject and body, and check that the privacy status is Encrypted. After sending the message, the test verifies that the sent message has the correct content and privacy status.
#
#In a subsequent test, the user saves a draft message with a specific subject and body, verifies that the privacy status is Undefined, and then goes to the drafts folder to open the saved message. The test checks that the message content matches what was saved and that the privacy status has been updated to secure. Finally, the user discards the message and returns to the Inbox.
  Scenario: Cucumber Save Draft email with long text

    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longText in the messageBody field
    Then I check the privacy status is Encrypted
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with longText
    And I check the privacy status is Encrypted
    And I discard the message
    And I go back to the Inbox




