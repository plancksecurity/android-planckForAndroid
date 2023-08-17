Feature: Test
  Background:




    #Summary: This Cucumber test scenario involves a user selecting an account and verifying that the calendar date and body text of the last received message are correct.
#Description: The test starts by selecting an account and clicking the compose message button. Then, the user enters their own email address in the messageTo field and a calendar event is attached to the message. After that, the user clicks the send message button and waits for the new message to appear. The test is successful if the message is sent successfully and the new message is visible.
  Scenario: Cucumber Calendar Event

    And I click compose message
    And I enter myself in the messageTo field
    And I enter TM-130 in the messageSubject field
    And I enter ThisIsTheBody in the messageBody field
    And I attach calendarEvent
    When I click the send message button
    And I wait for the new message



    #Summary: This Cucumber test scenario involves a user selecting an account and verifying the correctness of the calendar and body text of the last received message.
#Description: The test verifies that the body text of the message is correct. This test ensures that the messaging system is functioning correctly and that messages are displayed as expected.
  Scenario: Cucumber Calendar Event2

    And I click the last message received
    And I check that the Calendar is correct and body text is ThisIsTheBody
    And I go back to the Inbox


    #Summary: This Cucumber test scenario involves a user composing a message and adding unreliable recipients. The test verifies that insecurity warnings appear when adding these recipients, and the message is discarded as a result.
#Description: This Cucumber test is designed to ensure that a messaging application warns users when sending messages to unreliable recipients. In this test, the user selects a specific account and clicks on the "compose message" button to create a new message. Then they enter three unreliable recipients in the messageTo field. The test checks if an unsecure warning is displayed, indicating that the recipients may not be trustworthy. This test is crucial to ensure the security of the messaging application and protect users from potential security threats.
  Scenario: Cucumber Unsecure Warning Test

    And I click compose message
    And I enter 3 unreliable recipients in the messageTo field
    Then I check unsecure warnings are there
    And I discard the message



    #Summary: This Cucumber test scenario involves a user composing a message, adding and removing unreliable recipients, and verifying that there are no insecurity warnings displayed in the application.
#Description: The test scenario starts with the user selecting an account represented by the <account> placeholder. Then, the user clicks the compose message button and enters their own address in the messageTo field.
#
#In the following steps, the user removes their own address by clicking the X button, and adds three unreliable recipients to the messageTo field. The user then removes the unreliable recipients by clicking the X button in reverse order.
#
#After removing all the recipients, the user checks for insecurity warnings. The test scenario is successful if the application does not display any warnings.
#
#The primary objective of this Cucumber test is to ensure that the application does not display insecurity warnings when a user removes all recipients from a message after adding unreliable recipients.
  Scenario: Cucumber Remove address clicking X button


    And I click compose message
    And I enter myself in the messageTo field
    Then I remove the 1 address clicking X button
    And I enter 3 unreliable recipients in the messageTo field
    Then I remove the 3 address clicking X button
    Then I remove the 2 address clicking X button
    Then I remove the 1 address clicking X button
    Then I check insecurity warnings are not there


#Summary: This Cucumber test describes a scenario where tests the widgets in the application.
#Description: This Cucumber test scenario is designed to test the functionality of messaging and widgets in an application.
#The test is useful in verifying that the application's messaging and widget functionality work as expected. The scenario is flexible as it allows for different accounts to be used by merely changing the placeholder.
  Scenario: Cucumber Widget

    And I send 1 messages to bot1 with subject WidgetTest and body TestingWid_gets
    Then I test widgets


