"""Optional MCP helper.

It does NOT write health data itself. It creates a HealthBridge deep link that
can be opened on the Android phone. This keeps Health Connect access local to
the device.
"""
from urllib.parse import urlencode
from uuid import uuid4

from mcp.server import MCPServer

mcp = MCPServer("HealthBridge")


@mcp.tool()
def make_nutrition_link(
    name: str,
    kcal: float,
    meal: str = "unknown",
    protein: float | None = None,
    fat: float | None = None,
    carbs: float | None = None,
    sugar: float | None = None,
    at: str | None = None,
) -> dict:
    """Create a one-tap Android deep link for writing nutrition to Health Connect."""
    q: dict[str, object] = {
        "id": str(uuid4()),
        "name": name,
        "kcal": kcal,
        "meal": meal,
        "autocommit": 1,
    }
    for key, value in {
        "protein": protein,
        "fat": fat,
        "carbs": carbs,
        "sugar": sugar,
        "at": at,
    }.items():
        if value is not None:
            q[key] = value

    uri = "healthbridge://nutrition?" + urlencode(q)
    return {"uri": uri, "instruction": "Open this URI on the Android device."}


if __name__ == "__main__":
    mcp.run(transport="streamable-http")
