Feature: Mail to new contact
  Background:
    Given I created an account


#Summary: This Cucumber test involves testing the privacy status and message sending functionality of a mail application. The test includes sending messages to existing and new contacts, checking the privacy status of messages, and verifying the message body of sent messages.
#Description: This Cucumber test involves composing and sending messages through an email client. The test includes multiple steps, such as checking the privacy status of a message, entering recipients, subjects and message bodies, sending messages, waiting for new messages to arrive, and comparing the message bodies with expected values. The test also covers scenarios where the recipient or subject fields are empty, and it verifies the privacy status of the messages in different scenarios.
#
#In summary, this test checks the basic functionality of composing and sending messages, as well as the privacy and security of the messages, ensuring they are sent to the correct recipient with the correct subject and message body.
  Scenario: Cucumber Mail to new contact

    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot1 in the messageTo field
    And I enter newContact in the messageSubject field
    And I enter bodyMailToNewContact in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I enter empty in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check the privacy status is Undefined
    When I enter bot1 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I enter bodyMailToNewContact2 in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I click the first message
    Then I check the privacy status is Encrypted
    And I go to the sent folder
    And I click the first message
    Then I check the privacy status is NotEncrypted
    And I compare messageBody with bodyMailToNewContact2


