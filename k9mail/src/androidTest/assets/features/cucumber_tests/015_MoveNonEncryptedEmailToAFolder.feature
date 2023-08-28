Feature: Move Non encrypted email to a folder
  Background:



    #Summary: the user sends two messages to a bot with different privacy statuses. They move one message to the "spam" folder and verify that the privacy status remains "NotEncrypted." The user successfully navigates between the Inbox and "spam" folders in the messaging system.
  #Description: the user interacts with a messaging system involving a bot. They send two messages with different privacy statuses: one encrypted and one not encrypted. The user then moves the non-encrypted message to the "spam" folder and verifies that the privacy status remains "NotEncrypted" after moving. Additionally, the user successfully navigates between the Inbox and "spam" folders in the messaging system. The scenario aims to test the system's handling of moving non-encrypted emails and folder navigation.

  Scenario: Cucumber Move Non encrypted email to a folder
    When I send 1 message to bot1 with subject Reset and body ThisMessageWillBeEncrypted
    When I send 1 message to bot1 with subject Reset and body ThisMessageWillBeNotEncrypted
    And I click the last message received
    Then I check the privacy status is NotEncrypted
    When I move the message to the folder spam
    And I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I check the privacy status is NotEncrypted
    And I press back
    And I go to inbox folder from navigation menu



