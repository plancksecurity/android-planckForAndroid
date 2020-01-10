Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account


  @login-scenarios
  Scenario: Test7 importSettingsCancelTest
    Given I press back
    Then I open menu
    And I select from screen import_export_action
    And I select from screen settings_import
    And I go back to app
    Then I remove account

