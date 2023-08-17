Feature: Mail with Upper case
  Background:



    #Summary: The scenario tests the messaging application's treatment of uppercase characters in message fields. It involves sending messages with uppercase subjects and bodies to a bot, checking and verifying the privacy status, and ensuring proper encryption for messages with uppercase components.
  #Description: This scenario is designed to test the behavior of a messaging application when handling uppercase characters in message fields. The steps involve composing and sending messages with uppercase subjects and bodies to a bot, checking and verifying the privacy status, and ensuring that messages with uppercase components are properly encrypted. The scenario aims to assess the application's ability to handle and process uppercase characters accurately and maintain consistent privacy and encryption standards.

  Scenario: Cucumber Mail with Upper case

    When I click compose message
    And I send 1 message to bot1 with subject UpperCase and body TestingUpperCase
    And I click compose message
    And I check the privacy status is Undefined
    And I enter bot1-UpperCase in the messageTo field
    And I enter UpperCaseTest in the messageSubject field
    And I enter RecipientInUpperCase in the messageBody field
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted


