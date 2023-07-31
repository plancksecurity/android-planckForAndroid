Feature: Search for email/s in the Inbox
  Background:



    #Summary: This Cucumber test describes a set of actions that a user takes on a messaging platform to send and search for messages with specific text. The test involves sending messages to different bots with specific subject and body text, composing and sending a message to oneself, and verifying that specific messages are found when searched for.
#Description: The test involves selecting an account, navigating to the inbox, sending messages to different recipients with specific subject and body text, composing messages to oneself with specific message subject and body text, and performing searches for specific messages with certain text. The test is considered successful if all searches return the expected number of messages.
  Scenario: Cucumber Search for email/s in the Inbox

    And I go to inbox folder from navigation menu
    And I send 2 messages to bot2 with subject 3messages and body textA
    And I send 1 messages to bot5 with subject 3messages and body textC
    And I click compose message
    And I enter myself in the messageTo field
    And I enter 1messages in the messageSubject field
    And I enter textB in the messageBody field
    And I click the send message button
    And I wait for the new message
    And I send 1 messages to bot2 with subject test and body textA
    And I send 1 messages to bot5 with subject subject and body textD
    And I click compose message
    And I enter myself in the messageTo field
    And I enter Subject in the messageSubject field
    And I enter textB in the messageBody field
    And I click the send message button
    And I wait for the new message
    Then I search for 3 messages with text 3messages
    Then I search for 1 message with text 1messages
    Then I search for 0 messages with text 0messages
    Then I search for 2 messages with text textB





