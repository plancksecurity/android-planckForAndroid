Feature: Mail to existing contact is encrypted
  Background:



#Summary: This Cucumber test describes a series of actions to test the privacy status and message body in a messaging application. The actions include selecting an account, composing and sending messages to various bots, checking the privacy status of each message, and comparing the message body to expected values.
#Description:This Cucumber test involves testing the privacy status of messages sent in a messaging application. The test involves selecting an account and composing messages to various bots and other recipients, checking the privacy status of each message, and comparing the message body to expected values.
#
#In more detail, the test begins by selecting an account and clicking the "compose message" button. The privacy status is checked and found to be undefined. A message is then sent to a bot, and the privacy status is checked again and found to be secure. Another message is sent to the same bot and the privacy status is checked and found to be undefined.
#
#Another message is sent to the same bot, and the privacy status is checked and found to be secure. The test then goes to the sent folder, clicks the last message received, and compares the message body to an expected value.
#
#Overall, this test verifies that messages are being sent securely to the intended recipients and that the privacy status is correctly updated based on the message's recipients and content.
  Scenario: Cucumber Mail to existing contact is encrypted

    And I click compose message
    Then I check the privacy status is Undefined
    When I send 1 message to bot1 with subject TM-10 and body TM-10
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter empty in the messageTo field
    Then I check the privacy status is Undefined
    When I send 1 message to bot1 with subject TM-10A and body TM-10Abody
    And I go to the sent folder
    And I click the last message received
    Then I check the privacy status is Encrypted
    And I compare messageBody with TM-10Abody
    And I go back to the Inbox

