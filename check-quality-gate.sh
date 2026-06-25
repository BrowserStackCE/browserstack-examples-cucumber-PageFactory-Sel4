#!/bin/bash
set -e # Exit immediately if a command exits with a non-zero status.

# --- Configuration ---
# These values are read from Azure DevOps Pipeline variables.
project_name="BrowserStack Cucumber TestNG"
build_name="$BROWSERSTACK_BUILD_NAME"
username="$BROWSERSTACK_USERNAME"
access_key="$BROWSERSTACK_ACCESS_KEY"

# --- Script Parameters ---
max_attempts=20 # Max number of times to poll the API (20 attempts * 30s = 10 minutes)
build_tags=""   # Optional: Set build tags if needed

# --- Script Functions ---

# Function to URL-encode a string
sanitize_name() {
    local name="$1"
    echo "$name" | sed 's/ /%20/g'
}

# Function to get the UUID of the latest build
get_latest_build_uuid() {
    local sanitized_project_name
    local sanitized_build_name
    sanitized_project_name=$(sanitize_name "$project_name")
    sanitized_build_name=$(sanitize_name "$build_name")

    # API call relies on project and build name, which is most reliable in CI.
    local url="https://api-automation.browserstack.com/ext/v1/builds/latest?project_name=$sanitized_project_name&build_name=$sanitized_build_name"
    
    # Add optional build tags to the query if they are set
    if [ -n "$build_tags" ]; then
        local sanitized_build_tags
        sanitized_build_tags=$(sanitize_name "$build_tags")
        url="$url&build_tags=$sanitized_build_tags"
    fi
    
    echo "Querying for build..." >&2
    
    # Fetch the JSON response. This goes to standard output.
    curl -s --retry 3 --connect-timeout 10 -u "$username:$access_key" "$url"
}

# Function to get the status of the Quality Gate for a given build UUID
get_quality_gate_result() {
    local build_uuid="$1"
    local qg_url="https://api-automation.browserstack.com/ext/v1/quality-gates/$build_uuid"
    
    # Fetch the JSON response. This goes to standard output.
    curl -s -H --retry 3 --connect-timeout 10 -u "$username:$access_key" "$qg_url"
}

# --- Main Script Execution ---

echo "--- BrowserStack Quality Gate Check ---" >&2

build_info_json=$(get_latest_build_uuid)
build_uuid=$(echo "$build_info_json" | jq -r '.build_id')

if [ "$build_uuid" == "null" ] || [ -z "$build_uuid" ]; then
    echo "Error: Failed to retrieve a valid Build UUID from the API." >&2
    echo "API Response: $build_info_json" >&2
    exit 1
fi

echo "Successfully found Build UUID: $build_uuid" >&2
echo "Waiting 20 seconds before polling Quality Gate..." >&2
sleep 20

# Polling loop
attempt=0
while [[ $attempt -lt $max_attempts ]]; do
    attempt=$((attempt + 1))
    echo "Polling attempt #$attempt..." >&2
    
    quality_gate_json=$(get_quality_gate_result "$build_uuid")
    status=$(echo "$quality_gate_json" | jq -r '.status')
    
    echo "Current status is: '$status'" >&2
    
    if [ "$status" != "running" ]; then
        echo "Final Quality Gate Result: $quality_gate_json"
        
        if [ "$status" == "passed" ]; then
            echo "✅ Quality Gate Passed" >&2
            exit 0
        else
            echo "❌ Quality Gate Failed" >&2
            exit 1
        fi
    fi
    
    sleep 30
done

echo "Error: Timed out waiting for Quality Gate results after $max_attempts attempts." >&2
exit 1
