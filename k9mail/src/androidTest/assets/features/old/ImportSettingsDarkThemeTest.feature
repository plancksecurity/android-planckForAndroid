Feature: Test
    This is the first cucumber test

    Background:
        Given I create an account

  @login-scenarios
  Scenario: Test9 ImportSettingsDarkThemeTest
    Given Set external mock settingsthemedark
    Then I press back
    And I open menu
    And I select from screen import_export_action
    And I select from screen settings_import


