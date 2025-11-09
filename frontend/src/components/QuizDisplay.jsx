import { motion } from "framer-motion";

export default function QuizDisplay({ questions }) {
  return (
    <motion.div
      className="mt-6 space-y-4"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.4 }}
    >
      <h2 className="text-xl font-semibold text-gray-800 mb-3">🧩 Generated Questions</h2>
      {questions.map((q, i) => (
        <motion.div
          key={i}
          className="p-4 border rounded-xl bg-gray-50 shadow-sm hover:shadow-md transition"
          whileHover={{ scale: 1.02 }}
        >
          <p className="font-medium mb-2">{i + 1}. {q.question}</p>
          <ul className="ml-4 list-disc text-gray-700">
            {q.options.map((opt, j) => (
              <li key={j}>{opt}</li>
            ))}
          </ul>
          <p className="text-green-700 mt-2">✅ Answer: {q.answer}</p>
        </motion.div>
      ))}
    </motion.div>
  );
}
