Feature: Reset Partner Key
  Background:


    #Summary: The scenario tests the messaging application's privacy and encryption features by sending messages, resetting the partner key, and verifying the privacy status. The desired outcome is to ensure that the privacy status is accurately reflected for each message, alternating between "NotEncrypted" and "Encrypted" as expected.
  #Description: This scenario is a test case for a messaging application's privacy and encryption features. It involves a series of steps where messages are sent, the partner key is reset, additional messages are composed and sent, and the privacy status is checked. The purpose of the scenario is to verify that the privacy status is accurately reflected for each message, ensuring proper encryption where necessary and absence of encryption in other cases. The scenario aims to assess the application's adherence to privacy and encryption requirements during message transmission and key management.

  Scenario: Cucumber Reset Partner Key
    When I send 1 messages to bot2 with subject ResetKey and body ResetPartnersKey
    And I click the last message received
    Then I reset partner key
    When I press back
    And I click compose message
    And I enter bot2 in the messageTo field
    And I enter ResetKey2 in the messageSubject field
    And I enter NewMessageAfterReset in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted
    When I press back
    And I click compose message
    And I enter bot2 in the messageTo field
    And I enter key_reset_partner in the messageSubject field
    And I enter PartnerResetsCommunication in the messageBody field
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is NotEncrypted


