Feature: Mail with Lower case
  Background:



    #Summary: The scenario tests the messaging application's treatment of lowercase characters in message fields. It involves sending messages with lowercase subjects and bodies to a bot, checking and verifying the privacy status, and ensuring proper encryption for messages with lowercase components.
#Description: This scenario aims to test how the messaging application handles lowercase characters in message fields. The steps include composing and sending messages with lowercase subjects and bodies to the recipient "bot1". It also involves checking the privacy status, which should initially be "Undefined", and then verifying that the privacy status transitions to "Encrypted" after sending and receiving the message. The scenario ensures that the application correctly handles lowercase characters in different message components, maintaining the expected privacy and encryption standards.

  Scenario: Cucumber Mail with Lower case

    When I click compose message
    And I send 1 message to bot1 with subject LowerCase and body TestingLowerCase
    And I click compose message
    And I check the privacy status is Undefined
    And I enter bot1-LowerCase in the messageTo field
    And I enter LowerCaseTest in the messageSubject field
    And I enter RecipientInLowerCase in the messageBody field
    Then I check the privacy status is Encrypted
    When I click the send message button
    And I wait for the message and click it
    Then I check the privacy status is Encrypted


