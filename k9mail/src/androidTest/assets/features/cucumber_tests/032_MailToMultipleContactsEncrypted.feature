Feature: Mail to multiple contacts encrypted
  Background:



#Summary: This Cucumber test is testing the privacy status of messages sent between different accounts and to different recipients, and whether the privacy status changes based on the recipients and the contents of the message.
#Description: This Cucumber test involves testing various aspects of composing and sending messages in an email application. The test checks the privacy status of messages being composed and sent to different recipients, as well as the privacy status of received messages. Additionally, the test checks the correctness of the message body and subject, and verifies that messages can be sent and received successfully. The test also involves navigating between different folders, such as the inbox and sent folder. Overall, the test aims to ensure that the email application functions correctly and maintains the privacy of user data.
  Scenario: Cucumber Mail to multiple contacts encrypted

    And I click compose message
    Then I check the privacy status is Undefined
    When I send 1 message to bot1 with subject TM-11 and body TM-11
    And I click the last message received
    Then I check the privacy status is Encrypted
    When I press back
    And I click compose message
    And I send 1 message to bot2 with subject TM-11A and body TM-11A
    And I click the last message received
    Then I check the privacy status is Encrypted
    When I press back
    And I click compose message
    And I enter bot1 in the messageTo field
    And I enter bot2 in the messageTo field
    Then I check the privacy status is Encrypted
    When I enter TM-11B in the messageSubject field
    And I enter TM-11B in the messageBody field
    And I enter empty in the messageTo field
    Then I check the privacy status is Undefined
    When I enter bot2 in the messageTo field
    And I enter empty in the messageSubject field
    And I enter empty in the messageBody field
    Then I check the privacy status is Encrypted
    And I enter TM-11C in the messageSubject field
    And I enter TM-11CBody in the messageBody field
    When I click the send message button
    And I wait for the new message
    And I go to sent folder from navigation menu
    And I click the last message received
    Then I check the privacy status is Encrypted
    And I compare messageBody with TM-11CBody
    And I go back to the Inbox

