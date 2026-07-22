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

USERNAME_PATTERN = re.compile(r'^[A-Za-z0-9_.\-]{1,50}$')

USER_AGENTS = [
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4.1 Mobile/15E148 Safari/604.1",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
]

def identify_account_existence(username):
    """Signature Detection Logic for Big 3 + LinkedIn"""
    results = []
    
    # 1. INSTAGRAM
    try:
        url = f"https://www.instagram.com/{username}/"
        res = requests.get(url, headers={"User-Agent": USER_AGENTS[0]}, timeout=8)
        if res.status_code == 200 and "/login/" not in res.url.lower():
            results.append({"platform": "Instagram", "url": url})
    except: pass

    # 2. FACEBOOK
    try:
        url = f"https://www.facebook.com/{username}"
        res = requests.get(url, headers={"User-Agent": USER_AGENTS[1]}, timeout=8)
        if res.status_code == 200 and "login" not in res.url.lower():
            results.append({"platform": "Facebook", "url": url})
    except: pass

    # 3. LINKEDIN
    try:
        url = f"https://www.linkedin.com/in/{username}"
        res = requests.get(url, headers={"User-Agent": USER_AGENTS[1]}, timeout=8, allow_redirects=False)
        if res.status_code == 200:
            results.append({"platform": "LinkedIn", "url": url})
    except: pass

    return results  # THIS WAS MISSING

@app.route('/scan_username', methods=['GET'])
def scan_username():
    username = request.args.get('username', '').strip()
    if not USERNAME_PATTERN.match(username):
        return jsonify({"error": "Invalid username characters"}), 400

    # 1. Start Sherlock Engine
    cmd = ["sherlock", username, "--csv", "--timeout", "5", "--folderoutput", RESULTS_DIR]
    
    try:
        subprocess.run(cmd, shell=False, timeout=250) # Increased timeout
    except subprocess.TimeoutExpired:
        print("Sherlock took too long, proceeding with partial results.")

    found = []
    csv_path = os.path.join(RESULTS_DIR, f"{username}.csv")
    BLACKLIST = ["BoardGameGeek", "omg.lol", "Archive.org", "Giphy"]

    if os.path.exists(csv_path):
        try:
            with open(csv_path, newline='', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    name = row.get("name")
                    if row.get("exists", "").lower() == "claimed" and name not in BLACKLIST:
                        found.append({"platform": name, "url": row.get("url_user")})
            os.remove(csv_path)
        except: pass
    
    # 2. Run Pro Booster
    existing_platforms = [item['platform'] for item in found]
    pro_finds = identify_account_existence(username)
    
    # pro_finds is now a list, so this loop will work!
    for item in pro_finds:
        if item['platform'] not in existing_platforms:
            found.append(item)

    return jsonify({"platforms_found": found, "count": len(found)})

@app.route('/check_email_breach', methods=['GET'])
def check_email_breach():
    email = request.args.get('email', '').strip().lower()
    try:
        resp = requests.get(f"https://api.xposedornot.com/v1/check-email/{email}", timeout=10)
        if resp.status_code == 200:
            data = resp.json()
            breaches = data.get("breaches", [[]])[0]
            return jsonify({"status": "SUCCESS", "exposed": True, "breach_count": len(breaches), "breaches": breaches})
        return jsonify({"status": "SUCCESS", "exposed": False, "breach_count": 0, "breaches": []})
    except:
        return jsonify({"status": "ERROR", "message": "Breach API Timeout"}), 502

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)