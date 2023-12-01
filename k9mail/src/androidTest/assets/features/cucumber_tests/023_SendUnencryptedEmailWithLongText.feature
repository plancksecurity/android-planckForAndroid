Feature: Send Unencrypted email with long text
  Background:



#Summary: This Cucumber test involves sending an email with a long text message to a bot and then verifying that the message was sent and received correctly.
#Description: This Cucumber test involves sending an email with a long body of text to a bot, checking the privacy status of the email, and then comparing the message body with the original text. The test also checks whether the message is saved and sent correctly by going to the sent folder and comparing the message body with the original text. The test is conducted on a specified account and uses the subject and message body fields to send the email. Overall, the test ensures that emails can be sent and saved correctly with the desired privacy settings.
  Scenario: Cucumber Send Unencrypted email with long text

    When I click compose message
    And I check the privacy status is Undefined
    And I enter bot2 in the messageTo field
    And I enter TM152 in the messageSubject field
    And I enter longText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageBody from json file with longText
    When I go back to the Inbox
    And I go to sent folder from navigation menu
    And I click the first message
    Then I compare messageBody with longText



