ToDos:
@Hannes&Alex:
- If care display is not used, add output (device, gui etc.) to sendContentToOutputDevice in DemonstratorMain.
- If needed, add loop that requests more than one recommendation (in specific intervals or after a user input)
(Gemeinsame GUI? Empfehlung anzeigen + Möglichkeit Rating abzugeben + Button "Neue Empfehlung")

- If you need weather condition, add json-file and update code in getCurrentWeatherCondition in ContextModel.

@Alex:
- Add MongoDB for user model & update config-file (config_test.json)
- Add case-based filter (e.g. in RecommendationFilter), filter will be called in reactToCurrentContext in DemonstratorMain

ToDos before run:
- optional: change json-file with required trigger message (DemonstratorMain end of constructor)
- optional: change user mood to required mood (sendContentToOutputDevice in DemonstratorMain) (could be swapped out to config-file or additional json-file)

Run DemonstratorMain without any arguments (=> loads config_test.json (current settings: no XMPP, no MongoDB, all needed context information loaded from json-files or set in code)
