from flask import Flask, request, jsonify
import pickle

app = Flask(__name__)

# Load the brain
model = pickle.load(open("phishing_model.pkl", "rb"))
vectorizer = pickle.load(open("vectorizer.pkl", "rb"))

@app.route('/')
def home():
    return "Secure Sphere Server is Online!"

@app.route('/scan', methods=['POST'])
def scan():
    data = request.json
    url = data.get('url', '')
    if not url: return jsonify({"status": "Error"})
    
    # Ask the brain
    prediction = model.predict(vectorizer.transform([url]))[0]
    result = "Safe" if prediction == 0 else "Danger"
    
    return jsonify({"status": result})

if __name__ == '__main__':
    app.run(debug=True)
  
