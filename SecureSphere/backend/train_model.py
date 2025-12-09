import pickle
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression

# 1. Fake data to make the AI work immediately
urls = [
    "google.com", "youtube.com", "wikipedia.org", "amazon.com", 
    "secure-login.bank.com", "update-password-now.xyz", "free-iphone.net", 
    "verify-account-security.com", "suspicious-link.ru"
]
# 0 is Safe, 1 is Danger
labels = [0, 0, 0, 0, 1, 1, 1, 1, 1]

# 2. Train the model
print("Training...")
vectorizer = TfidfVectorizer()
X = vectorizer.fit_transform(urls)
model = LogisticRegression()
model.fit(X, labels)

# 3. Save the files
with open("phishing_model.pkl", "wb") as f: pickle.dump(model, f)
with open("vectorizer.pkl", "wb") as f: pickle.dump(vectorizer, f)
print("DONE! Files created.")
