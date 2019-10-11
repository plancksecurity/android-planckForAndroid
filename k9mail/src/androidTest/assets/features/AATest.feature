Feature: Test
  Background:
    Given I created an account
	#Description: Test if pEp changes to Privacy Status from “Unknown” to “Unsecure”
	# when entering the email address of a new contact bot1.
	# Also verify if pEp attaches my public key to outgoing messages
	# Assumption: No emails with the communication partner have been exchanged so far
	# Expectation: Privacy Status of outgoing message is “Unsecure”
  @TM-6 @TM-1 @TM-559
  Scenario: Cucumber Mail to new contact

    When I save test report