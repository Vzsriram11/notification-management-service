#!/bin/bash
RESPONSE=$(curl -s -X POST http://localhost:8080/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "test-018",
    "sourceSystem": "test-system",
    "eventId": "evt-018",
    "notificationType": "ALERT",
    "severity": "HIGH",
    "priority": "NORMAL",
    "recipientRefs": ["user-2"],
    "requestedChannels": ["EMAIL", "SMS"]
  }')
echo "POST response: $RESPONSE"

ID=$(echo "$RESPONSE" | grep -o '"notificationId":"[^"]*"' | cut -d'"' -f4)
echo "Fetching status for $ID"
curl -s http://localhost:8080/notifications/$ID/status
echo
