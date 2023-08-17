Feature: Copy email to a folder
  Background:



#Summary: This test sends a message to a bot, copies it to the "spam" folder, and performs comparisons on the message content.
  #Description: The test involves sending a message to a bot with a specific subject and body. It verifies and interacts with the received message, including copying it to the "spam" folder. The test also navigates between the "spam" folder and the inbox, comparing message content and performing other navigation actions.

  Scenario: Cucumber Copy email to a folder
    When I send 1 message to bot1 with subject copyThisMessage and body ThisMessageWillCopiedToAnotherFolder
    And I click the last message received
    And I copy the message to the folder spam
    Then I compare messageBody from json file with ThisMessageWillCopiedToAnotherFolder
    When I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I compare messageBody from json file with ThisMessageWillCopiedToAnotherFolder
    And I press back
    And I go to inbox folder from navigation menu


