#!/usr/bin/env bash

wget -O /usr/local/bin/scalastyle_2.12-1.0.0-batch.jar https://oss.sonatype.org/content/repositories/releases/org/scalastyle/scalastyle_2.12/1.0.0/scalastyle_2.12-1.0.0-batch.jar
echo "#!/bin/bash\n\
java -jar /usr/local/bin/scalastyle_2.12-1.0.0-batch.jar \"\$\@\"" > /usr/local/bin/scalastyle
chmod +x /usr/local/bin/scalastyle