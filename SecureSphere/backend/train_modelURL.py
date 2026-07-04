import pandas as pd
import pickle
import numpy as np
import re
import tldextract  # pip install tldextract
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.ensemble import RandomForestClassifier
from sklearn.utils import resample
from scipy.sparse import hstack

# ==========================================
# 1. TRUSTED DOMAIN LIST
# ==========================================
# Download a top-1m.csv from Tranco (https://tranco-list.eu) or Cisco Umbrella
# and place it in the same folder as this script.
print("Loading trusted domain list...")
TOP_DOMAINS = set(pd.read_csv("top-1m.csv", header=None)[1])

# ==========================================
# 2. FEATURE EXTRACTOR
# ==========================================
def get_url_numeric_features(url):
    url = str(url).lower()
    ext = tldextract.extract(url)
    registered_domain = f"{ext.domain}.{ext.suffix}"
    return [
        len(url),
        url.count('.'),
        url.count('-'),
        url.count('/'),
        url.count('?'),
        url.count('='),
        sum(c.isdigit() for c in url) / len(url) if len(url) > 0 else 0,
        1 if "https" in url else 0,
        1 if re.search(r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}', url) else 0,
        1 if registered_domain in TOP_DOMAINS else 0,  # trusted-domain signal
    ]

# ==========================================
# 3. LOAD & BALANCE DATA
# ==========================================
print("Loading dataset...")
df = pd.read_csv("malicious_phish.csv")
df_safe = df[df['type'] == 'benign']
df_malicious = df[df['type'] != 'benign']

# Cap resampling to the smaller class size to avoid synthetic duplication
n_safe = min(100000, len(df_safe))
n_bad = min(100000, len(df_malicious))

df_final = pd.concat([
    resample(df_safe, n_samples=n_safe, replace=False, random_state=42),
    resample(df_malicious, n_samples=n_bad, replace=False, random_state=42)
])
df_final['label'] = df_final['type'].apply(lambda x: 0 if x == 'benign' else 1)

# ==========================================
# 4. VECTORIZE (Textual + Numeric)
# ==========================================
print("Extracting features...")
X_numeric = np.array([get_url_numeric_features(u) for u in df_final['url']])
vectorizer = TfidfVectorizer(analyzer='char', ngram_range=(2, 5), max_features=5000)
X_tfidf = vectorizer.fit_transform(df_final['url'])

X_combined = hstack([X_tfidf, X_numeric])

# ==========================================
# 5. TRAIN
# ==========================================
print("Training Advanced Random Forest...")
model = RandomForestClassifier(n_estimators=100, max_depth=30, n_jobs=-1, random_state=42)
model.fit(X_combined, df_final['label'])

# ==========================================
# 6. SAVE
# ==========================================
pickle.dump(model, open("url_phishing_model.pkl", "wb"))
pickle.dump(vectorizer, open("url_vectorizer.pkl", "wb"))
print("SUCCESS: New URL-specific files created!")