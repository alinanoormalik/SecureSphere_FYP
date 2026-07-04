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

# Pattern updated to allow dots and underscores but strictly NO SPACES
USERNAME_PATTERN = re.compile(r'^[A-Za-z0-9_.\-]{1,50}$')
EMAIL_PATTERN = re.compile(r'^[^@\s]+@[^@\s]+\.[^@\s]+$')

USER_AGENTS = [
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
]

# ==========================================
# 1. EMAIL BREACH CHECK (Maximum Reliability)
# ==========================================
def try_xposedornot(email):
    url = f"https://api.xposedornot.com/v1/check-email/{email}"
    for i in range(2):
        try:
            headers = {"User-Agent": random.choice(USER_AGENTS), "Accept": "application/json", "Referer": "https://xposedornot.com/"}
            resp = requests.get(url, headers=headers, timeout=10)
            if resp.status_code == 200:
                data = resp.json()
                breaches = data.get("breaches", [[]])[0]
                return True, len(breaches) > 0, breaches
            if resp.status_code == 404: return True, False, []
            time.sleep(2)
        except: continue
    return False, None, None

def try_leakcheck(email):
    try:
        resp = requests.get("https://leakcheck.io/api/public", params={"check": email}, timeout=10)
        if resp.status_code == 200:
            data = resp.json()
            names = [s.get("name", "Unknown") for s in data.get("sources", [])]
            return True, data.get("found", 0) > 0, names
    except: pass
    return False, None, None

@app.route('/check_email_breach', methods=['GET'])
def check_email_breach():
    email = request.args.get('email', '').strip().lower()
    if not EMAIL_PATTERN.match(email):
        return jsonify({"status": "ERROR", "message": "Invalid email format"}), 400

    ok, exposed, names = try_xposedornot(email)
    if not ok: ok, exposed, names = try_leakcheck(email)
    
    if not ok:
        return jsonify({"status": "ERROR", "message": "Security APIs Throttled. Retry in 60s."}), 502
    
    return jsonify({
        "status": "SUCCESS",
        "exposed": exposed,
        "breach_count": len(names),
        "breaches": names,
        "source": "Global Breach Intelligence"
    })

# ==========================================
# 2. OSINT RECONNAISSANCE + PRO BOOSTER
# ==========================================
def identify_account_existence(username):
    """
    Signature Detection Logic for Big 3 (IG, FB, Twitter/X)
    """
    results = []
    
    # --- 1. INSTAGRAM ---
    try:
        ig_url = f"https://www.instagram.com/{username}/"
        res = requests.get(ig_url, headers={"User-Agent": USER_AGENTS[0]}, timeout=7)
        if res.status_code == 200 and "/login/" not in res.url.lower():
            results.append({"platform": "Instagram", "url": ig_url})
    except: pass

    # --- 2. FACEBOOK ---
    try:
        fb_url = f"https://www.facebook.com/{username}"
        res = requests.get(fb_url, headers={"User-Agent": USER_AGENTS[2]}, timeout=7)
        if res.status_code == 200 and "content_not_found" not in res.text:
            results.append({"platform": "Facebook", "url": fb_url})
    except: pass

    # --- 3. TWITTER / X ---
    try:
        tw_url = f"https://twitter.com/{username}"
        res = requests.get(tw_url, headers={"User-Agent": USER_AGENTS[0]}, timeout=7)
        # If not redirected to home/login and code is 200, it exists
        if res.status_code == 200 and "login" not in res.url.lower():
            results.append({"platform": "Twitter (X)", "url": tw_url})
    except: pass

    # --- 4. TIKTOK & SNAPCHAT ---
    extras = {
        "TikTok": f"https://www.tiktok.com/@{username}",
        "Snapchat": f"https://www.snapchat.com/add/{username}"
    }
    for name, url in extras.items():
        try:
            res = requests.head(url, headers={"User-Agent": USER_AGENTS[1]}, timeout=5)
            if res.status_code == 200:
                results.append({"platform": name, "url": url})
        except: continue

    return results

@app.route('/scan_username', methods=['GET'])
def scan_username():
    username = request.args.get('username', '').strip()
    if not USERNAME_PATTERN.match(username):
        return jsonify({"error": "Invalid format. No spaces allowed."}), 400

    # 1. Start Sherlock Engine
    cmd = ["sherlock", username, "--csv", "--timeout", "15", "--folderoutput", RESULTS_DIR]
    subprocess.run(cmd, shell=False, timeout=120)

    found = []
    csv_path = os.path.join(RESULTS_DIR, f"{username}.csv")
    
    # BLACKLIST: Removing all junk/unreliable sites as requested
    BLACKLIST = [
        "BoardGameGeek", "omg.lol", "Blitz Tactics", "GaiaOnline", 
        "Pokemon Showdown", "Archive.org", "Giphy", "Flashback",
        "Wikipedia", "mastodon.cloud", "mastodon.social", "mastodon.xyz", 
        "mstdn.social", "mstdn.io", "Vero"
    ]

    if os.path.exists(csv_path):
        with open(csv_path, newline='', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                platform = row.get("name")
                if row.get("exists", "").lower() == "claimed" and platform not in BLACKLIST:
                    found.append({"platform": platform, "url": row.get("url_user")})
        os.remove(csv_path)

    # 2. Run Advanced Booster for IG/FB/Twitter
    existing_platforms = [item['platform'] for item in found]
    pro_finds = identify_account_existence(username)
    
    for item in pro_finds:
        if item['platform'] not in existing_platforms:
            found.append(item)

    return jsonify({
        "platforms_found": found,
        "count": len(found),
        "status": "Maximum Accuracy Audit Complete"
    })

# ==========================================
# 3. REMEDIATION (Report Generator)
# ==========================================
@app.route('/get_report_template', methods=['GET'])
def get_report():
    platform = request.args.get('platform', 'Support')
    url = request.args.get('url', 'Unknown')
    template = (
        f"OFFICIAL REPORT: Identity Integrity Violation\n"
        f"Platform: {platform}\n"
        f"URL: {url}\n\n"
        f"I am reporting an unauthorized profile clone that is impersonating my "
        f"identity and utilizing my digital assets without consent. "
        f"Please verify this footprint against your safety standards."
    )
    return jsonify({"template": template})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)