Feature: Send Encrypted email with long subject
  Background:



  #Summary: The scenario tests sending an encrypted email with a long subject using the messaging application. It involves composing and sending a message with a short subject and body, checking the privacy status, entering a long subject and body, sending the message, comparing the subject and body with values from a JSON file, and verifying the sent message in the sent folder.
  #Description: This scenario focuses on testing the messaging application's capability to send encrypted emails with long subjects. It involves composing and sending a message with a short subject and body, checking the privacy status, entering a long subject and body, sending the message, comparing the subject and body with values from a JSON file, and verifying the sent message in the sent folder. The scenario ensures that the application can handle and deliver encrypted emails with long subject lines accurately and securely. It also verifies that the sent message's subject and body match the expected values and confirms the preservation of privacy throughout the email transmission process.

  Scenario: Cucumber Send Encrypted email with long subject

    When I click compose message
    And I send 1 message to bot1 with subject firstEmail and body ThisIsTheBody
    And I click compose message
    And I enter bot1 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter longSubject in the messageSubject field
    #And I enter AnyText in the messageBody field
    And I click the send message button
    And I wait for the message and click it
    Then I compare messageSubject from json file with longSubject
    #Then I compare messageBody from json file with AnyText
    Then I check the privacy status is Encrypted
    When I go back to the Inbox
    And I go to sent folder from navigation menu
    And I click the first message
    Then I compare messageSubject with longSubject


