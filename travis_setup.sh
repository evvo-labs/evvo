#!/usr/bin/env bash

wget -O /home/travis/bin/scalastyle_2.12-1.0.0-batch.jar https://oss.sonatype.org/content/repositories/releases/org/scalastyle/scalastyle_2.12/1.0.0/scalastyle_2.12-1.0.0-batch.jar
echo "#!/bin/bash\n\
java -jar /home/travis/bin/scalastyle_2.12-1.0.0-batch.jar \"\$\@\"" > /home/travis/bin/scalastyle
chmod +x /home/travis/bin/scalastyle
export PATH="$PATH:/home/travis/bin"