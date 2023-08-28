Feature: Calendar Event
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



