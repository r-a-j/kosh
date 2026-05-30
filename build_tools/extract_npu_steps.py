import json
import os

log_path = r"C:\Users\raj24\.gemini\antigravity\brain\fa600799-6459-4e76-9794-a49692705dc6\.system_generated\logs\transcript.jsonl"

steps_found = []
with open(log_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            data = json.loads(line)
            # Find steps before the recent run (step_index < 700)
            if data.get("source") == "MODEL" and data.get("type") in ("PLANNER_RESPONSE", "USER_INPUT"):
                content = data.get("content", "")
                if any(kw in content for kw in ["cmake", "dispatch_api", "Qualcomm", "NPU", "git", "delegate"]):
                    steps_found.append((data.get("step_index"), data.get("type"), content))
        except Exception as e:
            pass

print(f"Total matches found: {len(steps_found)}")
for idx, stype, content in steps_found:
    if idx < 710:  # Only print older steps
        print(f"=== STEP {idx} ({stype}) ===")
        print(content[:1000])  # print up to 1000 chars of each
        print("=" * 40)
