## Disclaimer: This is a 220% time project, don't expect anything "just works"
## Quick Start
Generate bulk upload

`clj -m gobbler "/somewhere/debug.log" > lines.json`

Send it to Elastic Search (expected to have the mapping below)

`curl -XPOST "http://localhost:9200/logs/_doc/_bulk?pretty" -H 'Content-Type: application/json' --data-binary @lines.json`

## ES Mapping
```json
PUT /logs
PUT /logs/_mapping/_doc
{
    "properties": {
        "action": {
            "type": "keyword"
        },
        "adapter_version": {
            "type": "text"
        },
        "app": {
            "type": "keyword"
        },
        "command_line_args": {
            "type": "text"
        },
        "core_sha": {
            "type": "text"
        },
        "electron_version": {
            "type": "text"
        },
        "frame_id": {
            "type": "text"
        },
        "log-uuid": {
            "type": "keyword",
            "fields": {
                "keyword": {
                    "type": "keyword",
                    "ignore_above": 256
                }
            }
        },
        "manifest": {
            "type": "text"
        },
        "message": {
            "type": "text"
        },
        "node-version": {
            "type": "text"
        },
        "openfin_version": {
            "type": "keyword"
        },
        "timestamp": {
            "type": "date",
            "format": "yyyy-MM-dd k:m:s.SSS"
        },
        "v8_version": {
            "type": "text"
        },
        "win": {
            "type": "keyword"
        }
    }
}
```
