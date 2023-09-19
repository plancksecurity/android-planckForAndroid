Feature: Handshake in new Message
  Background:
    Given I created an account

#Summary: the user composes and sends a message to a bot with specific content, checks and confirms privacy status and trust words, and verifies the updated privacy status as "Trusted."
#Description: the user interacts with a messaging system to send a message to a bot. They then perform various actions to check and ensure the privacy and trustworthiness of the communication. The scenario involves verifying privacy status, confirming trust words, and ultimately confirming that the privacy status is updated as "Trusted."
  Scenario: Cucumber Handshake in new Message
    When I click compose message
    And I send 1 message to bot3 with subject TM-16 and body TM-16body
    And I click the last message received
    And I compare rating_string from json file with unencrypted
    And I go back to the Inbox
    And I click compose message
    And I enter bot3 in the messageTo field
    Then I check if the privacy status is Encrypted
    And I confirm trust words match
    When I click confirm trust words
    Then I discard the message
 
