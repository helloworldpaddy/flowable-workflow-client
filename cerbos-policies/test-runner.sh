#!/bin/bash

# Cerbos Policy Test Runner
# This script runs the Cerbos policy tests to validate authorization rules

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POLICIES_DIR="$SCRIPT_DIR"
TESTS_DIR="$SCRIPT_DIR/tests"

echo "🔍 Running Cerbos Policy Tests"
echo "==============================================="
echo "Policies directory: $POLICIES_DIR"
echo "Tests directory: $TESTS_DIR"
echo ""

# Check if cerbos CLI is available
if ! command -v cerbos &> /dev/null; then
    echo "❌ Cerbos CLI not found. Please install cerbos CLI first."
    echo "   Installation: https://docs.cerbos.dev/cerbos/latest/installation.html"
    exit 1
fi

echo "✅ Cerbos CLI found: $(cerbos version)"
echo ""

# Validate policy files
echo "📋 Validating policy files..."
if cerbos compile "$POLICIES_DIR" --output /dev/null; then
    echo "✅ All policy files are valid"
else
    echo "❌ Policy validation failed"
    exit 1
fi

echo ""

# Run tests
echo "🧪 Running policy tests..."

# Use array to handle filenames with spaces properly
readarray -t TEST_FILES < <(find "$TESTS_DIR" -name "*.yaml" -type f)

if [ ${#TEST_FILES[@]} -eq 0 ]; then
    echo "⚠️  No test files found in $TESTS_DIR"
    exit 0
fi

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

for test_file in "${TEST_FILES[@]}"; do
    echo "Running tests from: $(basename "$test_file")"
    
    # Capture both stdout and stderr for better error reporting
    if test_output=$(cerbos run test --policy-dir "$POLICIES_DIR" "$test_file" 2>&1); then
        echo "✅ $(basename "$test_file"): PASSED"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo "❌ $(basename "$test_file"): FAILED"
        echo "Error details:"
        echo "$test_output" | sed 's/^/  /'
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo ""
done

# Summary
echo "==============================================="
echo "📊 Test Summary"
echo "==============================================="
echo "Total test files: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo "🎉 All tests passed!"
    exit 0
else
    echo "💥 $FAILED_TESTS test(s) failed!"
    exit 1
fi