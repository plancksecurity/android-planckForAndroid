Feature: Move email to a folder
  Background:


    #Summary: This test involves sending a message to a bot. It verifies the received message, moves it to the "spam" folder, and compares the message body with an expected value from a JSON file.
  #Description: The test involves sending a message to a bot. It verifies and interacts with the received message, including moving it to the "spam" folder. The test also navigates between the "spam" folder and the inbox and comparing message content.
  Scenario: Cucumber Move email to a folder
    When I send 1 message to bot1 with subject moveThisMessage and body ThisMessageWillMovedToAnotherFolder
    And I click the last message received
    And I move the message to the folder spam
    And I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I compare messageBody from json file with ThisMessageWillMovedToAnotherFolder
    And I press back
    And I go to inbox folder from navigation menu

