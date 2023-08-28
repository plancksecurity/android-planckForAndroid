Feature: Unsecure Warning Test
  Background:


    #Summary: This Cucumber test scenario involves a user composing a message and adding unreliable recipients. The test verifies that insecurity warnings appear when adding these recipients, and the message is discarded as a result.
#Description: This Cucumber test is designed to ensure that a messaging application warns users when sending messages to unreliable recipients. In this test, the user selects a specific account and clicks on the "compose message" button to create a new message. Then they enter three unreliable recipients in the messageTo field. The test checks if an unsecure warning is displayed, indicating that the recipients may not be trustworthy. This test is crucial to ensure the security of the messaging application and protect users from potential security threats.
  Scenario: Cucumber Unsecure Warning Test

    And I click compose message
    And I enter 3 unreliable recipients in the messageTo field
    Then I check unsecure warnings are there
    And I discard the message

