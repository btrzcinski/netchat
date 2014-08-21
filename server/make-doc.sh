#!/bin/bash

# Clean out any old docs.
if [ -d doc ]; then
    echo -e "Removing old docs\n"
    rm -rf doc
fi

# Generate docs.
echo -e "Generating docs\n"
rdoc -d --inline-source --line-numbers -x rails -x utils -x testing --main start.rb

