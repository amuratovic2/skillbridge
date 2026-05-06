URL="http://localhost:3003/api/orders/1"
COUNT=100
SUCCESS=0
FAIL=0

echo "=== Load Balancing Test: $COUNT zahtjeva ==="
START=$(date +%s%N)

for i in $(seq 1 $COUNT); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "x-user-id: 1" \
    "$URL")
  if [ "$STATUS" == "200" ] || [ "$STATUS" == "404" ]; then
    SUCCESS=$((SUCCESS + 1))
  else
    FAIL=$((FAIL + 1))
  fi
done

END=$(date +%s%N)
DURATION=$(( (END - START) / 1000000 ))

echo "Uspješnih zahtjeva: $SUCCESS / $COUNT"
echo "Neuspješnih: $FAIL"
echo "Ukupno vrijeme: ${DURATION}ms"
echo "Prosjek po zahtjevu: $((DURATION / COUNT))ms"
echo ""
echo "Provjeri logove svake instance za distribuciju:"
echo "  - Instanca 1 (port 3003): pogledaj terminal 1"
echo "  - Instanca 2 (port 3004): pogledaj terminal 2"