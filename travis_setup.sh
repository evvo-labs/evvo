#!/usr/bin/env bash

wget -O /usr/bin/scalastyle_2.12-1.0.0-batch.jar https://oss.sonatype.org/content/repositories/releases/org/scalastyle/scalastyle_2.12/1.0.0/scalastyle_2.12-1.0.0-batch.jar
echo "#!/bin/bash\n\
java -jar /usr/bin/scalastyle_2.12-1.0.0-batch.jar \"\$\@\"" > /usr/bin/scalastyle
chmod +x /usr/bin/scalastyle