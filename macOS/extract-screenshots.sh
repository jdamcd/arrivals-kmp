#!/bin/bash
# Extract UI test screenshots from the latest xcresult bundle.
# Usage: ./extract-screenshots.sh [output-dir]
# Screenshots are saved as PNG files named after the test attachment name.

set -euo pipefail

DERIVED_DATA="$HOME/Library/Developer/Xcode/DerivedData"
OUTPUT_DIR="${1:-/tmp/ui-test-screenshots}"

# Find the latest Arrivals xcresult
RESULT=$(find "$DERIVED_DATA" -path "*/Arrivals-*/Logs/Test/*.xcresult" -type d | sort | tail -1)
if [ -z "$RESULT" ]; then
    echo "No Arrivals xcresult found in DerivedData"
    exit 1
fi
echo "Using: $RESULT"

mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.png

# Get all test IDs
TEST_IDS=$(xcrun xcresulttool get test-results tests --path "$RESULT" | python3 -c "
import json, sys
data = json.load(sys.stdin)
def find_tests(nodes):
    for node in nodes:
        if 'children' in node:
            find_tests(node['children'])
        elif 'nodeIdentifier' in node:
            print(node['nodeIdentifier'])
find_tests(data.get('testNodes', []))
")

# For each test, extract attachments
for test_id in $TEST_IDS; do
    xcrun xcresulttool get test-results activities --test-id "$test_id" --path "$RESULT" 2>/dev/null | python3 -c "
import json, sys, subprocess, os
data = json.load(sys.stdin)
output_dir = '$OUTPUT_DIR'
result_path = '$RESULT'

def find_attachments(obj):
    if isinstance(obj, dict):
        if 'attachments' in obj:
            for a in obj['attachments']:
                name = a.get('name', 'unknown')
                payload_id = a.get('payloadId', '')
                if payload_id and name.endswith('.png'):
                    # Strip UUID suffix from name
                    clean_name = name.rsplit('_', 2)[0] + '.png' if '_' in name else name
                    out_path = os.path.join(output_dir, clean_name)
                    subprocess.run([
                        'xcrun', 'xcresulttool', 'export', '--legacy',
                        '--type', 'file', '--path', result_path,
                        '--id', payload_id, '--output-path', out_path
                    ], capture_output=True)
                    print(f'  Exported: {clean_name}')
        for v in obj.values():
            find_attachments(v)
    elif isinstance(obj, list):
        for i in obj:
            find_attachments(i)
find_attachments(data)
" 2>/dev/null
done

echo ""
echo "Screenshots saved to: $OUTPUT_DIR"
ls -1 "$OUTPUT_DIR"/*.png 2>/dev/null || echo "No screenshots found"
