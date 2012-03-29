@ECHO OFF

REM Taverna startup script

REM distribution directory
set TAVERNA_HOME=%~dp0

REM 300 MB memory, 140 MB for classes
set ARGS=-Xmx300m -XX:MaxPermSize=140m

REM Taverna system properties
set ARGS=%ARGS% "-Draven.profile=file:%TAVERNA_HOME%conf/current-profile.xml"
set ARGS=%ARGS% -Djava.system.class.loader=net.sf.taverna.raven.prelauncher.BootstrapClassLoader 
set ARGS=%ARGS% -Draven.launcher.app.main=net.sf.taverna.t2.commandline.CommandLineLauncher
set ARGS=%ARGS% -Draven.launcher.show_splashscreen=false
set ARGS=%ARGS% -Djava.awt.headless=true
set ARGS=%ARGS% "-Dtaverna.startup=%TAVERNA_HOME%."
IF NOT x%RAVEN_APPHOME%==x SET ARGS=%ARGS% "-Draven.launcher.app.home=%RAVEN_APPHOME%"

java %ARGS% -jar "%TAVERNA_HOME%lib\prelauncher-2.3.jar" %*
