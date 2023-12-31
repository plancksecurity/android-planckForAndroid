Feature: Handshake wrong trustwords
  Background:
    Given I created an account


#Summary: the user sends messages with incorrect trust words to a bot and clicks to stop trusting. The privacy status is checked and found to be "Dangerous" for both messages.
#Description: the user engages in interactions with a messaging system involving a bot. They send messages with incorrect trust words, leading to a privacy status of "Dangerous." The user then verifies this status for the messages, both when stopping trusting and when sending new messages. The scenario aims to test the system's handling of incorrect trust words and the correct updating of the privacy status.
  Scenario: Cucumber Handshake wrong trustwords
    When I send 1 message to bot1 with subject TM-19 and body handshakeWrongTrustwords
    And I click the last message received
    Then I click mistrust words
    Then I check if the privacy status is Dangerous
    When I go back to the Inbox
    And I send 1 message to bot1 with subject TM-19A and body handshakeWrongTrustwordsA
    Then I check the badge color of the first message is Dangerous
    When I click the last message received
    Then I check if the privacy status is Dangerous

