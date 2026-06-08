import pickle
from sklearn.feature_extraction.text import CountVectorizer
from flask import Flask, request, jsonify

app = Flask(__name__)

# ===============================
# Load AI Models
# ===============================
try:
    spam_model = pickle.load(open("email_model.pkl", "rb"))
    spam_cv = pickle.load(open("vectorizer.pkl", "rb"))
    print("AI Loaded!")
except Exception as e:
    print(f"AI Files not found: {e}")

model = pickle.load(open("phishing_model.pkl", "rb"))
vectorizer = pickle.load(open("vectorizer.pkl", "rb"))


# ===============================
# Routes
# ===============================
@app.route('/')
def home():
    return "Secure Sphere Server is Online!"


@app.route('/scan', methods=['POST'])
def scan():
    data = request.json
    url = data.get('url', '')
    if not url: 
        return jsonify({"status": "Error"})
    
    # Ask the brain
    prediction = model.predict(vectorizer.transform([url]))[0]
    result = "Safe" if prediction == 0 else "Danger"
    
    return jsonify({"status": result})


@app.route('/predict-email', methods=['POST'])
def predict_email():
    data = request.json
    text = data.get('text', '')
    
    if not text:
        return jsonify({"result": "Error"})
        
    # Convert text to numbers using the saved vectorizer
    vec_text = spam_cv.transform([text])
    # Predict
    prediction = spam_model.predict(vec_text)
    
    # Result is 'spam' or 'ham'
    return jsonify({"result": prediction[0]})


# ===============================
# Execution Block
# ===============================
if __name__ == '__main__':
    app.run(debug=True)