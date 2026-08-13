#!/usr/bin/env python3
import argparse
import uuid
from urllib.parse import urlencode

p = argparse.ArgumentParser(description="Create a HealthBridge deep link")
p.add_argument("--name", required=True)
p.add_argument("--kcal", required=True, type=float)
p.add_argument("--protein", type=float)
p.add_argument("--fat", type=float)
p.add_argument("--carbs", type=float)
p.add_argument("--sugar", type=float)
p.add_argument("--meal", choices=["unknown", "breakfast", "lunch", "dinner", "snack"], default="unknown")
p.add_argument("--at", help="ISO-8601 with offset, e.g. 2026-08-13T20:48:00+03:00")
p.add_argument("--no-autocommit", action="store_true")
a = p.parse_args()

q = {
    "id": str(uuid.uuid4()),
    "name": a.name,
    "kcal": a.kcal,
    "meal": a.meal,
    "autocommit": 0 if a.no_autocommit else 1,
}
for key in ("protein", "fat", "carbs", "sugar", "at"):
    value = getattr(a, key)
    if value is not None:
        q[key] = value

print("healthbridge://nutrition?" + urlencode(q))
