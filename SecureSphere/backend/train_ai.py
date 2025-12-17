# train_ai.py
import pandas as pd
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.model_selection import train_test_split
from sklearn.linear_model import LogisticRegression
import pickle

print("1. Loading Data...")
# We use latin-1 encoding to prevent errors
try:
    df = pd.read_csv("spam.csv", encoding="latin-1")
except:
    print("ERROR: spam.csv not found! Is it in the same folder?")
    exit()

# Keep only label and message
df = df[['v1', 'v2']]
df.columns = ['label', 'message']

print("2. Converting words to numbers...")
cv = CountVectorizer()
x = cv.fit_transform(df['message'])
y = df['label']

print("3. Training the Brain...")
# Split 80% training, 20% testing
x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=0.2, random_state=42)

model = LogisticRegression()
model.fit(x_train, y_train)

print("4. Saving files...")
pickle.dump(model, open("email_model.pkl", "wb"))
pickle.dump(cv, open("vectorizer.pkl", "wb"))

print("SUCCESS! Brain created.")
