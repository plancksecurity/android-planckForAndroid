Feature: Attachment Send 4 Files To 3 Contacts Part1
  Background:


#Summary: This Cucumber test involves sending multiple messages to different bots with various subjects and bodies, and attaching several files to the last message sent.
#Description: This Cucumber test is a functional test that tests the ability of a messaging system to send multiple messages to different recipients with different subject and message body. It also checks if different types of files can be attached to the message, and the system's ability to send the message successfully. The test involves sending three messages to different recipients with different subjects and message bodies, and then sending another message to the same recipients with a different subject and message body. The test attaches different types of files to the message and then checks if the message has been sent successfully.
  Scenario: Cucumber Attachment Send 4 Files To 3 Contacts Part1

    When I click compose message
    And I send 1 message to bot6 with subject TM-128 and body attach4
    When I click compose message
    And I send 1 message to bot2 with subject TM-128A and body attach4A
    When I click compose message
    And I send 1 message to bot5 with subject TM-128B and body attach4B
    When I click compose message
    And I enter bot6 in the messageTo field
    And I enter bot2 in the messageTo field
    And I enter bot5 in the messageTo field
    And I enter TM-128C in the messageSubject field
    And I enter attach4C in the messageBody field
    When I attach PDF
    And I attach MSoffice
    And I attach settings
    And I attach picture
    And I click the send message button
    And I wait for the new message



