SETLOCAL

set MW_HOME=@WEBLOGIC_HOME@/..
set WL_HOME=@WEBLOGIC_HOME@
set DOMAIN_HOME=@SERVER_ROOT@
call "%WL_HOME%\server\bin\setWLSEnv.cmd" %*
java weblogic.WLST %1%

ENDLOCAL
