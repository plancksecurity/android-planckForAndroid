Feature: Special Characters
  Background:



#Summary: This Cucumber test involves checking the privacy status of an email, composing and sending an email with special characters to bot, comparing the message body with the special characters, going to the sent folder, and comparing the message body again. The test also involves composing and sending an email with special characters to oneself and comparing the message body.
#Description: This Cucumber test checks the functionality of composing and sending messages with special characters in the message body. It also tests the ability to view sent messages and compare the message body with the original message.
#
#The test begins with selecting an account and clicking on the compose message button. It then checks the initial privacy status of the message. Next, the test enters the recipient and message details, attaches the message body with special characters, and sends the message.
#
#The test then waits for the message to arrive, clicks on it, and compares the message body with the original message. It also checks the ability to view sent messages and compares the message body with the original message.
#
#Finally, the test goes to the inbox, composes a new message with the same special characters, sends the message, waits for it to arrive, clicks on it, and compares the message body with the original message.

  Scenario: Cucumber Special Characters

    And I click compose message
    Then I check the privacy status is Undefined
    When I enter bot2 in the messageTo field
    And I enter Special1 in the messageSubject field
    And I enter specialCharacters in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with specialCharacters
    And I press back
    And I go to sent folder from navigation menu
    And I click the first message
    Then I compare messageBody with specialCharacters
    When I press back
    And I go to inbox folder from navigation menu
    And I click compose message
    And I enter myself in the messageTo field
    And I enter Special2 in the messageSubject field
    And I enter specialCharacters in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody with specialCharacters


