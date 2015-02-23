#!/bin/sh

set -e

# 300 MB memory, 140 MB for classes
memlimit=-Xmx300m
permsize=-XX:MaxPermSize=140m

## Parse the command line to extract the pieces to move around to before or
## after the JAR filename...
pre=-Djava.awt.headless=true 
post=
for arg
do
    case $arg in
	-JXmx*) memlimit=`echo $arg | sed 's/-JX/-X/'` ;;
	-JXX:MaxPermSize=*) permsize=`echo $arg | sed 's/-JXX/-XX/'` ;;
	-J*) pre="$pre `echo $arg | sed 's/-J/-/'`" ;;
	-D*) pre="$pre $arg" ;;
	*) post="$post \"$arg\"" ;;
    esac
done
if test "xx" = "x${post}x"; then
    echo "Missing arguments! Bug in argument processing?" >&2
    exit 1
fi
eval set x $post
shift

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
APPHOME_PROP= 
if test x != "x$TAVERNA_APPHOME"; then
    APPHOME_PROP="-Dtaverna.app.home=$TAVERNA_APPHOME"
fi
RUNID_PROP= 
if test x != "x$TAVERNA_RUN_ID"; then
    RUNID_PROP="-Dtaverna.runid=$TAVERNA_RUN_ID"
fi
INTERACTION_PROPS=-Dtaverna.interaction.ignore_requests=true
if test x != "x$INTERACTION_HOST"; then
    INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.host=$INTERACTION_HOST"
    INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.port=$INTERACTION_PORT"
    INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.webdav_path=$INTERACTION_WEBDAV"
    INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.feed_path=$INTERACTION_FEED"
    if test x != "x$INTERACTION_PUBLISH"; then
    	INTERACTION_PROPS="$INTERACTION_PROPS -Dtaverna.interaction.publishAddressOverride=$INTERACTION_PUBLISH"
    fi
fi

MainClass=net.sf.taverna.t2.commandline.CommandLineLauncher

echo "pid:$$"
exec "$javabin" $memlimit $permsize \
  "-Dlog4j.configuration=file://$taverna_home/conf/log4j.properties " \
  "-Djava.util.logging.config.file=$taverna_home/conf/logging.properties " \
  "-Dtaverna.app.startup=$taverna_home" -Dtaverna.interaction.ignore_requests=true \
  $APPHOME_PROP $RUNID_PROP $INTERACTION_PROPS -Djava.awt.headless=true \
  -Dcom.sun.net.ssl.enableECC=false -Djsse.enableSNIExtension=false $pre \
  -jar "$taverna_home/lib/taverna-command-line-0.1.1.jar" \
  ${1+"$@"}
