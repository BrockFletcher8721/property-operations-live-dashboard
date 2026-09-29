#!/bin/sh
set -eu
classes="${TMPDIR:-/tmp}/property-dashboard-classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java -name '*.java')
java -cp "$classes" com.infrai.property.PropertyDashboardRunner
