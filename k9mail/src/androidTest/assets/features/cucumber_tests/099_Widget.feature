Feature: Widget
  Background:


#Summary: This Cucumber test describes a scenario where tests the widgets in the application.
#Description: This Cucumber test scenario is designed to test the functionality of messaging and widgets in an application.
#The test is useful in verifying that the application's messaging and widget functionality work as expected. The scenario is flexible as it allows for different accounts to be used by merely changing the placeholder.
  Scenario: Cucumber Widget

    And I send 1 messages to bot1 with subject WidgetTest and body TestingWid_gets
    Then I test widgets


