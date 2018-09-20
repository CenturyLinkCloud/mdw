@echo off
start cmd /c javaw -jar %* %MDW_HOME%\mdw-cli.jar run ^>^> mdw.log ^2^>^&^1
