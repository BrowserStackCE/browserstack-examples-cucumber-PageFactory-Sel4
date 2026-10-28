#Author: Abdul Qadir Khan
#Date: 20/12/21
#Description: Demonstrate Cucumber Java framework with BS Automate product
@users @regression
Feature: Different Users use cases in BStackDemo
  Background:
    Given User is on home page
    When User clicks on sign in link

 @noimage @fail
 Scenario Outline: TC-5883 Login as User with no image loaded
   When User enters <username> and <password> and clicks on sign in
   And <username> is signed in to the App
   Then Product images are not loaded for the <username>
   Examples:
     | username | password |
     |"image_not_loading_user"|"testingisfun99"|

  @orders @SRA
  Scenario Outline: TC-5884 Login as existing user to verify orders
     When User enters <username> and <password> and clicks on sign in
     And <username> is signed in to the App
     And User clicks on Orders
     Then Existing orders are shown for the <username>
     And Screen reader is enabled on the orders page
     And User captures 10 spoken outputs with the screen reader
     Then Screen reader spoken outputs are captured in order
     And Screen reader first spoken output is the expected demo text
     Examples:
       | username | password |
       |"existing_orders_user"|"testingisfun99"|

  @locked
  Scenario Outline: TC-5885 Login as a locked User
      When User enters <username> and <password> and clicks on sign in
      Then <username> gets Your account has been locked.
      Examples:
        | username | password |
        |"locked_user"|"testingisfun99"|
