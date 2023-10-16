Feature: Attachment Send 1 File To 1 Contact Part1
  Background:
    Given I created an account



#Summary: This Cucumber test involves removing all messages, sending a message with attachments containing special characters to bot5, and waiting for a new message.
#Description: This Cucumber test is focused on sending a message with special characters and checking that it is successfully received. The test begins by selecting an account and removing all existing messages. Then, a new message is composed and sent to a bot with a specified subject and body, which contains special characters. The message is sent with an attachment containing special characters as well. Finally, the test checks that the message was successfully received by waiting for a new message to appear.
  Scenario: Cucumber Attachment Send 1 File To 1 Contact Part1

    And I remove all messages
    And I click compose message
    And I send 1 message to bot5 with subject TM-126 and body attach1
    And I click compose message
    And I enter bot5 in the messageTo field
    And I enter TM-126A in the messageSubject field
    And I enter attachSpecialCharacters in the messageBody field
    When I attach specialCharacters
    And I click the send message button
    And I wait for the new message



