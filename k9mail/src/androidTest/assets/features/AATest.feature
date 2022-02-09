Feature: Test
  Background:
    Given I created an account

 # @QTR-1979
 # Scenario Outline: Cucumber Widget
#
 #   When I select account <account>
 #   And I click compose message
 #   And I send 1 messages to bot1 with subject WidTest and body TestingWid_gets
 #   Then I test widgets
#
 #   Examples:
 #     |account|
 #     |  0    |

 # @QTR-report
 # Scenario Outline: Cucumber Report
#
 #   When I save test report
#
 #   Examples:
 #     | account |
 #     | 0 |
#
   @QTR
   Scenario Outline: Cucumber Sync Stress

  #   When Normal use of sync for devices A and B for 7 days
     When I reset my own key
     Examples:
       | account |
       | 0       |



