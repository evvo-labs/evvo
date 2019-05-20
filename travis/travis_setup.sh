#!/usr/bin/env bash

# We need an executable called `scalastyle` for overcommit to be able to run the scalastyle tests
# The jar is available at this URL, we just have to place it in the right directory and
# make the executable have the right name, be in the PATH, and have the executable bit flipped.
wget -O /home/travis/bin/scalastyle_2.12-1.0.0-batch.jar https://oss.sonatype.org/content/repositories/releases/org/scalastyle/scalastyle_2.12/1.0.0/scalastyle_2.12-1.0.0-batch.jar
cp travis/travis_scalastyle_executable.sh /home/travis/bin/scalastyle
chmod +x /home/travis/bin/scalastyle
export PATH="$PATH:/home/travis/bin"