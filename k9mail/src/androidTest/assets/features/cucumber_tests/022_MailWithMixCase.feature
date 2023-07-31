Feature: Mail with Mix case
  Background:



    #Summary: The scenario tests the messaging application's treatment of mixed-case characters in message fields. It involves sending messages with mixed-case subjects and bodies to a bot, checking and verifying the privacy status, and ensuring proper encryption for messages with mixed-case components.
  #Description: This scenario aims to test the behavior of a messaging application when dealing with mixed-case characters in message fields. The steps include composing and sending messages with mixed-case subjects and bodies. It also involves checking the privacy status, which should initially be "Undefined", and then verifying that the privacy status remains "Encrypted" throughout the process. The scenario ensures that the application correctly handles mixed-case characters in different message components, maintains consistent privacy and encryption standards, and accurately processes and delivers messages with mixed-case content.

  Scenario: Cucumber Mail with Mix case

    When I click compose message
    And I send 1 message to bot1 with subject MixCase and body TestingMixCase
    And I click compose message
    And I check the privacy status is Undefined
    And I enter bot1-MixCase in the messageTo field
    And I enter MixCaseTest in the messageSubject field
    And I enter RecipientInMixCase in the messageBody field
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted


