Feature: Mail to multiple contacts (mixed)
  Background:



#Summary: This Cucumber test checks the privacy status of messages being sent to different recipients in different scenarios.
#Description: This Cucumber test involves sending messages to different bots and checking the privacy status of the message. The test starts with selecting an account and composing and sending messages to bot1 and bot2. Then, the privacy status is checked and the message is sent to bot5, and the privacy status is checked again.
#
#Next, a message is composed with an empty "to" field and sent to bot1 and bot2, with privacy status checked before and after adding bot5 to the recipient list. Finally, the last message received is opened from the sent folder, and its privacy status and body content are checked.
  Scenario: Cucumber Mail to multiple contacts (mixed)

    And I click compose message
    And I send 1 message to bot1 with subject TM-12 and body TM-12
    And I click compose message
    And I send 1 message to bot2 with subject TM-12A and body TM-12A
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter bot5 in the messageTo field
    Then I check the privacy status is NotEncrypted
    When I enter TM-12B in the messageSubject field
    And I enter TM-12B in the messageBody field
    And I enter empty in the messageTo field
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter bot5 in the messageTo field
    Then I check the privacy status is NotEncrypted
     When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted
    And I compare rating_string from json file with unencrypted
    And I go back to the Inbox
    And I go to sent folder from navigation menu
    And I click the last message received
    Then I check the privacy status is NotEncrypted
    And I compare messageBody with TM-12B
