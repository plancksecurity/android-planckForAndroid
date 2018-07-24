Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

    @login-scenarios
    Scenario: Test2 attachFilesToMessage
        Given I click message compose
        Then I fill messageTo field with self
        And I fill messageSubject field with subject
        And I fill messageBody field with bodyMessage
        Then Set external mock settings
        Then I attach files to message
        And I click send message button
        And I remove account



