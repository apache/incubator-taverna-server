#!/bin/sh

set -e

## resolve links - $0 may be a symlink
prog="$0"

real_path() {
    readlink -m "$1" 2>/dev/null || python -c 'import os,sys;print os.path.realpath(sys.argv[1])' "$1"
}

realprog=`real_path "$prog"`
taverna_home=`dirname "$realprog"`
javabin=java
if test -x "$JAVA_HOME/bin/java"; then
    javabin="$JAVA_HOME/bin/java"
fi
RAVEN_APPHOME_PROP= 
if test x != "x$RAVEN_APPHOME"; then
    RAVEN_APPHOME_PROP="-Draven.launcher.app.home=$RAVEN_APPHOME"
fi
RUNID_PROP= 
if test x != "x$TAVERNA_RUN_ID"; then
    RUNID_PROP="-Dtaverna.runid=$TAVERNA_RUN_ID"
fi
INTERACTION_PROPS= 
if test x != "x$INTERACTION_HOST"; then
    INTERACTION_PROPS="-Dtaverna.interaction.host=$INTERACTION_HOST"
    INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.port=$INTERACTION_PORT"
    INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.webdav_path=$INTERACTION_WEBDAV"
    INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.feed_path=$INTERACTION_FEED"
fi

# 300 MB memory, 140 MB for classes
exec "$javabin" -Xmx300m -XX:MaxPermSize=140m \
  "-Draven.profile=file://$taverna_home/conf/current-profile.xml" \
  "-Dtaverna.startup=$taverna_home" $RAVEN_APPHOME_PROP $RUNID_PROP \
  $INTERACTION_PROPS \
  -Djava.system.class.loader=net.sf.taverna.raven.prelauncher.BootstrapClassLoader \
  -Draven.launcher.app.main=net.sf.taverna.t2.commandline.CommandLineLauncher \
  -Draven.launcher.show_splashscreen=false \
  -Djava.awt.headless=true \
  -jar "$taverna_home/lib/"prelauncher-*.jar \
  ${1+"$@"}
