Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

    @login-scenarios
    Scenario: Test2 attachFilesToMessage
        Given I click message compose
        Then Set external mock settings
        Then I attach files to new message true and send it
        And I remove account



