#!/bin/bash

# Wipe the entire Choreboo Cloud SQL database (all users, all data)
# This is a development utility for resetting a shared dev environment.
# 
# WARNING: This deletes all data from the remote database.
#          Firebase Auth user records are NOT deleted.
#
# Usage: ./reset-db.sh

echo "=========================================="
echo "Choreboo Database Reset Utility"
echo "=========================================="
echo ""
echo "⚠️  WARNING: This will DELETE ALL data from the remote database."
echo "   - All user records"
echo "   - All habits and habit logs"
echo "   - All Choreboos"
echo "   - All households"
echo ""
echo "Firebase Auth user records are NOT deleted."
echo ""
read -p "Type 'yes' to confirm: " confirmation

if [ "$confirmation" != "yes" ]; then
  echo "Cancelled."
  exit 0
fi

echo ""
echo "Opening Cloud SQL shell..."
echo ""
echo "Once connected, run this command (copy/paste):"
echo ""
echo "  TRUNCATE \"habit_log\", \"habit\", \"choreboo\", \"user\", \"household\" CASCADE;"
echo "  \\q"
echo ""
echo "=========================================="
echo ""

npx firebase-tools@latest dataconnect:sql:shell \
  --service choreboo-dataconnect \
  --location us-central1

echo ""
echo "=========================================="
echo "Database reset complete."
echo ""
echo "Next steps:"
echo "  1. Sign out of the app (or clear local Room data in Settings)"
echo "  2. Delete your Firebase Auth user from Firebase Console (optional)"
echo "  3. Re-register with your email"
echo "  4. Create a new Choreboo in onboarding"
