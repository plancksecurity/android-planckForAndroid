Feature: Mail from new contact encrypted
  Background:



#Summary: This Cucumber test checks the encryption and privacy status of a message sent to a bot, verifies the message contents and then replies to it with additional text.
#Description: This Cucumber test is a set of steps that a series of actions to test the behavior of an email client. The test focuses on the encryption and privacy features of the email client, and involves sending and receiving emails to different contacts, and checking their privacy status. The test also involves comparing the message body with pre-defined values from a JSON file.
#
#The test begins by selecting an account and clicking on the compose message button. The privacy status is then checked to ensure that it is undefined. The user then sends a message to bot with a specific subject and body, and the privacy status is checked again to ensure that it is Encrypted. The user then sends another message to bot1 with a different subject and body, and the privacy status is checked again to ensure that it is still secure.
#
#The test then involves clicking on the last message received, and checking the privacy status again, followed by replying to the message and checking the privacy status once more. The user then enters some additional text in the message subject and body fields, and clicks the send message button. Finally, the user goes back to the inbox and waits for a new message.
  Scenario: Cucumber Mail from new contact encrypted

    And I click compose message
    And I send 1 message to bot1 with subject mailFromNewContactEncryptedBody and body MailFromNewContactEncryptedBody
    And I click the last message received
    Then I compare messageBody from json file with MailFromNewContactEncryptedBody
    And I check the privacy status is Encrypted
    When I click reply message
    Then I check the privacy status is Encrypted
    And I click the send message button
    And I go back to the Inbox
    And I wait for the new message


