// src/data/mockQuizData.js

export const mockQuizzes = {
  "AI Basics": {
    topic: "AI Basics",
    questions: [
      {
        questionId: 1,
        question: "What does AI stand for?",
        options: ["Artificial Intelligence", "Advanced Integration", "Automated Input", "Applied Information"],
        answer: "Artificial Intelligence",
      },
      {
        questionId: 2,
        question: "Which of the following is a subfield of AI?",
        options: ["Machine Learning", "Cloud Computing", "Networking", "Cryptography"],
        answer: "Machine Learning",
      },
      {
        questionId: 3,
        question: "Which algorithm is used in supervised learning?",
        options: ["K-Means", "Decision Tree", "Apriori", "DBSCAN"],
        answer: "Decision Tree",
      },
      {
        questionId: 4,
        question: "What is the main goal of AI?",
        options: ["To make machines think like humans", "To create random outputs", "To store data efficiently", "To automate networking"],
        answer: "To make machines think like humans",
      },
      {
        questionId: 5,
        question: "Which company developed AlphaGo?",
        options: ["DeepMind", "OpenAI", "Google AI", "IBM Watson"],
        answer: "DeepMind",
      },
    ],
  },

  "Python Fundamentals": {
    topic: "Python Fundamentals",
    questions: [
      {
        questionId: 1,
        question: "Which keyword is used to define a function in Python?",
        options: ["func", "def", "lambda", "function"],
        answer: "def",
      },
      {
        questionId: 2,
        question: "Which data type is immutable in Python?",
        options: ["List", "Set", "Dictionary", "Tuple"],
        answer: "Tuple",
      },
      {
        questionId: 3,
        question: "Which of these creates a virtual environment?",
        options: ["python -v", "pip install env", "python -m venv", "python virtual"],
        answer: "python -m venv",
      },
      {
        questionId: 4,
        question: "What does PEP stand for?",
        options: ["Python Enhancement Proposal", "Programming Environment Protocol", "Python Execution Process", "Parallel Event Processing"],
        answer: "Python Enhancement Proposal",
      },
      {
        questionId: 5,
        question: "Which of these is used for iteration?",
        options: ["for loop", "class", "return", "def"],
        answer: "for loop",
      },
    ],
  },

  "Database Systems": {
    topic: "Database Systems",
    questions: [
      {
        questionId: 1,
        question: "What does SQL stand for?",
        options: ["Structured Query Language", "System Query Logic", "Sequential Query Language", "Simple Query Logic"],
        answer: "Structured Query Language",
      },
      {
        questionId: 2,
        question: "Which command is used to remove a table from a database?",
        options: ["DELETE", "REMOVE", "DROP", "TRUNCATE"],
        answer: "DROP",
      },
      {
        questionId: 3,
        question: "What type of key uniquely identifies a record in a table?",
        options: ["Foreign Key", "Primary Key", "Super Key", "Candidate Key"],
        answer: "Primary Key",
      },
      {
        questionId: 4,
        question: "Which normal form removes partial dependency?",
        options: ["1NF", "2NF", "3NF", "BCNF"],
        answer: "2NF",
      },
      {
        questionId: 5,
        question: "Which SQL clause is used to filter records?",
        options: ["ORDER BY", "WHERE", "GROUP BY", "FROM"],
        answer: "WHERE",
      },
    ],
  },

  Mathematics: {
    topic: "Mathematics",
    questions: [
      {
        questionId: 1,
        question: "What is the value of π (pi) up to two decimal places?",
        options: ["3.12", "3.14", "3.15", "3.10"],
        answer: "3.14",
      },
      {
        questionId: 2,
        question: "What is the derivative of sin(x)?",
        options: ["cos(x)", "-cos(x)", "-sin(x)", "tan(x)"],
        answer: "cos(x)",
      },
      {
        questionId: 3,
        question: "If f(x)=x², what is f'(2)?",
        options: ["2", "4", "8", "1"],
        answer: "4",
      },
      {
        questionId: 4,
        question: "What is the area of a circle with radius r?",
        options: ["πr²", "2πr", "r²/π", "πr"],
        answer: "πr²",
      },
      {
        questionId: 5,
        question: "What is 7 multiplied by 6?",
        options: ["36", "42", "48", "56"],
        answer: "42",
      },
    ],
  },

  "General Knowledge": {
    topic: "General Knowledge",
    questions: [
      {
        questionId: 1,
        question: "Who wrote the national anthem of India?",
        options: ["Rabindranath Tagore", "Mahatma Gandhi", "Subhash Chandra Bose", "Sarojini Naidu"],
        answer: "Rabindranath Tagore",
      },
      {
        questionId: 2,
        question: "What is the capital of Japan?",
        options: ["Beijing", "Tokyo", "Seoul", "Bangkok"],
        answer: "Tokyo",
      },
      {
        questionId: 3,
        question: "Which is the largest ocean on Earth?",
        options: ["Atlantic", "Pacific", "Indian", "Arctic"],
        answer: "Pacific",
      },
      {
        questionId: 4,
        question: "Which planet is known as the Red Planet?",
        options: ["Venus", "Earth", "Mars", "Jupiter"],
        answer: "Mars",
      },
      {
        questionId: 5,
        question: "What is the national currency of the United Kingdom?",
        options: ["Euro", "Dollar", "Pound Sterling", "Franc"],
        answer: "Pound Sterling",
      },
    ],
  },
};
