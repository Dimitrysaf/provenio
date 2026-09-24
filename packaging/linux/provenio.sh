#!/bin/sh
# Starts the desktop app on the bundled Java runtime. The engine's JNI library sits in the
# resources directory the app loads native code from.
exec /app/jre/bin/java \
    -Xmx1g \
    -Dcompose.application.resources.dir=/app/lib/provenio \
    -Djava.library.path=/app/lib/provenio \
    -Dawt.useSystemAAFontSettings=on \
    -jar /app/lib/provenio/provenio.jar "$@"
