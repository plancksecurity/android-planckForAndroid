Feature: Remove address clicking X button
  Background:


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


