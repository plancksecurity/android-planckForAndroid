Feature: Save Draft email with long word
  Background:



#Summary: This Cucumber test describes a scenario where a user selects an account and performs various actions related to composing and managing messages. The test covers sending messages to bots, checking the privacy status of messages, replying to messages, saving messages as drafts, and discarding drafts.
#Description: This Cucumber test is focused on testing various aspects of sending and receiving emails using an email client. The test covers scenarios such as selecting accounts, sending messages to bots, checking privacy status, replying to messages, saving drafts, and discarding drafts. The test also involves comparing message bodies with expected values, checking the privacy status of messages, and navigating between different folders such as Inbox, Sent, and Drafts.
#
#The overall objective of the test is to ensure that the email client functions as expected, providing a secure and reliable way for users to send and receive messages.
  Scenario: Cucumber Save Draft email with long word

    When I send 1 message to bot2 with subject saveDraft and body saveDraftBody
    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot2 in the messageTo field
    And I enter TM154A in the messageSubject field
    And I enter longWord in the messageBody field
    And I save as draft
    And I go to the drafts folder
    And I click message at position 1
    Then I compare messageBody with longWord
    And I check the privacy status is Encrypted
    And I discard the message
    And I go back to the Inbox



