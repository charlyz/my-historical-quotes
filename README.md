# Start

```
sbt stage; /code/my-historical-quotes/target/universal/stage/bin/my-historical-quotes -J-Xmx1G -J-Xms1G
```

# Test

```
curl -X POST -H "Content-Type: application/json" -H "Cache-Control: no-cache" -d '{
  "ticker": "T",
  "limit": 3,
  "order": "date.asc",
  "offset": 0
}' "http://127.0.0.1:9000/get-quotes"
```