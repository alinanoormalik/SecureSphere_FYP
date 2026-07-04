import subprocess
import os
import csv
import time
import re
import random
import requests
from flask import Flask, request, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
RESULTS_DIR = os.path.join(BASE_DIR, "sherlock_results")
os.makedirs(RESULTS_DIR, exist_ok=True)

# No spaces allowed for usernames in OSINT
USERNAME_PATTERN = re.compile(r'^[A-Za-z0-9_.\-]{1,50}$')

USER_AGENTS = [
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0 Safari/537.36"
]

def identify_pro_existence(username):
    """Signature probing for IG, FB, and LinkedIn (WhatsMyName Logic)"""
    res_list = []
    
    # 1. INSTAGRAM (Redirect Probe)
    try:
        url = f"https://www.instagram.com/{username}/"
        r = requests.get(url, headers={"User-Agent": USER_AGENTS[0]}, timeout=8)
        if r.status_code == 200 and "/login/" not in r.url.lower():
            res_list.append({"platform": "Instagram", "url": url})
    except: pass

    # 2. FACEBOOK (Signature Probe)
    try:
        url = f"https://www.facebook.com/{username}"
        r = requests.get(url, headers={"User-Agent": USER_AGENTS[1]}, timeout=8)
        if r.status_code == 200 and "content_not_found" not in r.text and "login" not in r.url.lower():
            res_list.append({"platform": "Facebook", "url": url})
    except: pass

    # 3. LINKEDIN (Pre-Authwall Probe)
    try:
        url = f"https://www.linkedin.com/in/{username}"
        # If LinkedIn allows the request without redirecting to 'authwall', the profile exists
        r = requests.get(url, headers={"User-Agent": USER_AGENTS[1]}, timeout=8, allow_redirects=False)
        if r.status_code == 200:
            res_list.append({"platform": "LinkedIn", "url": url})
    except: pass

    return res_list

@app.route('/scan_username', methods=['GET'])
def scan_username():
    username = request.args.get('username', '').strip()
    if not USERNAME_PATTERN.match(username):
        return jsonify({"error": "No spaces allowed in username"}), 400

    # Run Main Engine
    cmd = ["sherlock", username, "--csv", "--timeout", "15", "--folderoutput", RESULTS_DIR]
    subprocess.run(cmd, shell=False, timeout=120)

    found = []
    # JUNK BLACKLIST: Removing the fake-positive/spammy sites
    JUNK_SITES = ["BoardGameGeek", "omg.lol", "Blitz Tactics", "GaiaOnline", "Pokemon Showdown", "Archive.org", "Giphy", "Wikipedia", "mastodon.social", "mstdn.io"]

    csv_path = os.path.join(RESULTS_DIR, f"{username}.csv")
    if os.path.exists(csv_path):
        with open(csv_path, newline='', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                name = row.get("name")
                if row.get("exists", "").lower() == "claimed" and name not in JUNK_SITES:
                    found.append({"platform": name, "url": row.get("url_user")})
        os.remove(csv_path)

    # Add Pro Probes (IG/FB/LinkedIn)
    existing = [p['platform'] for p in found]
    for p in identify_pro_existence(username):
        if p['platform'] not in existing:
            found.append(p)

    return jsonify({"platforms_found": found, "count": len(found)})

# ... (Keep your Email Breach and Password routes exactly as they were) ...

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)