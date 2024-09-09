Feature: Handshake in existing message
  Background:
    Given I created an account


    #Summary : the user composes a message to a bot with specific subject and body. They check and confirm that the message is not encrypted, then send it. After receiving a new message, they verify trust words, confirm the trust, and check that the privacy status is updated to "Trusted."
  #Description: the user interacts with a messaging system to send a message to a bot with a particular subject and body. They ensure that the message is not encrypted and proceed to send it. After receiving a new message, they verify trust words, confirm trust, and finally, check if the privacy status of the communication is updated to "Trusted." The scenario revolves around messaging, privacy, and trust verification.
  Scenario: Cucumber Handshake in existing message
    When I click compose message
    And I enter bot6 in the messageTo field
    And I enter TM-17 in the messageSubject field
    And I enter TM-17 in the messageBody field
    Then I check the privacy status is NotEncrypted
    When I click the send message button
    And I wait for the new message
    And I click the last message received
    Then I confirm trust words match
    When I click confirm trust words
    Then I check if the privacy status is Encrypted


