#!/bin/sh

echo Removing .log and .pyc and .class
sleep 1
find . -iname '*.log' -or -iname '*.pyc' -or -iname '*.class' -exec echo '{}' \;
find . -iname '*.log' -or -iname '*.pyc' -or -iname '*.class' | xargs rm 2>/dev/null || echo No files to delete

