@echo off
start cmd /c javaw -Dmdw.webpack.precompile=false -jar %* %MDW_HOME%\mdw-cli.jar run ^>^> mdw.log ^2^>^&^1
