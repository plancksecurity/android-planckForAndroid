Feature: Copy Non encrypted email to a folder
  Background:

    #Summary: The user sends two messages to a bot with different privacy statuses. They copy one non-encrypted message to the "spam" folder and verify that the privacy status remains "NotEncrypted." The user successfully navigates between the Inbox and "spam" folders in the messaging system.
  #Description: The user interacts with a messaging system involving a bot. They send two messages, one encrypted and one not encrypted. The user then copies the non-encrypted message to the "spam" folder and confirms that the privacy status remains "NotEncrypted" after copying. Additionally, the user successfully navigates between the Inbox and "spam" folders in the messaging system. The scenario aims to test the system's handling of copying non-encrypted emails and folder navigation.

  Scenario: Cucumber Copy Non encrypted email to a folder
    When I send 1 message to bot1 with subject Reset and body ThisMessageWillBeEncrypted
    When I send 1 message to bot1 with subject Reset and body ThisMessageWillBeNotEncrypted
    And I click the last message received
    Then I check the privacy status is NotEncrypted
    When I copy the message to the folder spam
    And I go back to the Inbox
    And I go to spam folder from navigation menu
    And I click the first message
    Then I check the privacy status is NotEncrypted
    And I press back
    And I go to inbox folder from navigation menu
