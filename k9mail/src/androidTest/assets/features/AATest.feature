Feature: Test
  Background:
    Given I created an account


  @QTR-A
  Scenario Outline: Cucumber Save Report

    When I save test report
    Examples:
      |account|
      |  0    |

