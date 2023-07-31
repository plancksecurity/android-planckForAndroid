Feature: Calendar Event2
  Background:


    #Summary: This Cucumber test scenario involves a user selecting an account and verifying the correctness of the calendar and body text of the last received message.
#Description: The test verifies that the body text of the message is correct. This test ensures that the messaging system is functioning correctly and that messages are displayed as expected.
  Scenario: Cucumber Calendar Event2

    And I click the last message received
    And I check that the Calendar is correct and body text is ThisIsTheBody
    And I go back to the Inbox
