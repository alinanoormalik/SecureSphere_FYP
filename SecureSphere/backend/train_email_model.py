import pickle
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
# 1. Expanded Data
emails = [
    "Your Google account password was changed successfully.",
    "The meeting has been rescheduled to tomorrow at 10 AM.",
    "Attached is the project proposal for your review.",
    "Your Amazon order has been shipped and is on its way.",
    "Hi, I'm following up on the job application from last week.",
    "The weekly team sync is starting in 10 minutes.",
    "Please find the invoice for the month of October attached.",
    "Can you please send me the files for the presentation by EOD?",
    "URGENT: Your bank account has been locked. Click here to verify.",
    "Congratulations! You have won a $1000 Walmart Gift Card!",
    "Your PC is infected with a virus. Download this fix immediately.",
    "Final Notice: Your tax payment is overdue. Pay now.",
    "Get rich quick! Earn $5000 a week working from home.",
    "You have a pending inheritance of $10,000,000. Claim now.",
    "Account Alert: Someone tried to login to your account. Click to secure.",
    "Verify your Netflix account info or your subscription will be cancelled."
]
labels = [0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1] 

# 2. Train
vectorizer = TfidfVectorizer(stop_words='english')
X = vectorizer.fit_transform(emails)
model = LogisticRegression()
model.fit(X, labels)

# 3. Save files LOCALLY in VS Code
with open("email_model.pkl", "wb") as f:
    pickle.dump(model, f)
with open("email_vectorizer.pkl", "wb") as f:
    pickle.dump(vectorizer, f)

print("SUCCESS: Local files created in VS Code!")