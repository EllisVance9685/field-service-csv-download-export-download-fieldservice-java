#!/usr/bin/env sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
classes_dir=$(mktemp -d)
trap 'rm -rf "$classes_dir"' EXIT

find "$repo_dir/src/main/java" "$repo_dir/src/test/java" -name '*.java' -print > "$classes_dir/sources.txt"
javac -encoding UTF-8 -d "$classes_dir" @"$classes_dir/sources.txt"
java -cp "$classes_dir" learning.fieldservice.export.WorkOrderCsvDecisionTest
