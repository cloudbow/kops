Unavailable Tests
==================

# How to run 

cd to the directory in commandline

Type sbt 

You can run the project in 2 ways 

provide 

1. Provide household_id, cookie, rxId, *title, callsign and check for subscription 


run-main com.slingmedia.tests.VerifyLineupRequests ZslMabN5kcrf8rEnnReJivkTGTco6AoY _zeus_f9c3dfa0-d112-4bef-a8e4-a079b33b6aca dish1946221657 "NHL: Senators at Penguins" "KNTV"


2. Provide household_id, cookie, rxId, contentId and check for subscription 


household_id, cookie, rxId, contentId

example:

run-main com.slingmedia.tests.VerifyLineupRequests ZslMabN5kcrf8rEnnReJivkTGTco6AoY _zeus_f9c3dfa0-d112-4bef-a8e4-a079b33b6aca dish1946221657 "" "" 2695857






Results:

USER NOT SUBSCRIBED TO THE CONTENT!!! AND HENCE UNAVAILABLE -- User not subscribed for this content
NO CONTENT WITH THIS TITLE !! -- The title & callsign combination supplied is invalid
CONTENTIDS ARE NOT YET GENERATED! -- ContentIds are not yet generated for this

Note:
* > You can make basic regexp title search with this eg: you can pass Oriole*