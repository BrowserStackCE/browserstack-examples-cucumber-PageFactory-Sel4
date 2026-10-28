#Author: Abdul Qadir Khan
#Date: 14/12/21
#Description: Demonstrate Cucumber Java framework with BS Automate product
 @test @regression
  Feature: Offers for user in Mumbai area
    @offers
    Scenario Outline: Signed in user can see promotional offers under Offers for Mumbai Location
      Given User is on home page
      When User clicks on sign in link
      And User enters <username> and <password> and clicks on sign in
      And <username> is signed in to the App
      And User Clicks on Offers
      Then User can see offers list for Mumbai location
      Examples:
        | username | password |
        |"fav_user"|"testingisfun99"|