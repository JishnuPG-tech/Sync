import subprocess
import sys
import os

filename = sys.argv[1] if len(sys.argv) > 1 else "device_screen.png"
outpath = os.path.join(r"C:\Users\JISHNU PG\.gemini\antigravity\brain\2fe2aca2-50c2-4441-b718-21efc2285c0e", filename)

try:
    # Target emulator-5554 explicitly
    data = subprocess.check_output(["adb", "-s", "emulator-5554", "exec-out", "screencap", "-p"])
    with open(outpath, "wb") as f:
        f.write(data)
    print(f"Captured: {outpath} ({len(data)} bytes)")
except Exception as e:
    print(f"Error: {e}", file=sys.stderr)
    sys.exit(1)
