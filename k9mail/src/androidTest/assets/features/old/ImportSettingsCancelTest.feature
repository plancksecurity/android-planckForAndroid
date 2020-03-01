Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test8 ImportSettingsCancelTest
    Given Set external mock settings
    Then I press back
    And I press back
    Then I open menu
    And I select from screen import_export_action
    And I select from screen settings_import


