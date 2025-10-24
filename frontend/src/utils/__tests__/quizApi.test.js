import MockAdapter from "axios-mock-adapter";
import axios from "axios";
import { generateQuiz, submitQuiz } from "../quizApi";

const mock = new MockAdapter(axios);

describe("quizApi", () => {
  beforeEach(() => mock.reset());

  test("generateQuiz hits /quiz/generate with topic & difficulty", async () => {
    mock.onPost("/quiz/generate").reply(200, [{ text: "Q1" }]);
    const data = await generateQuiz("AI", "medium");
    expect(Array.isArray(data)).toBe(true);
  });

  test("submitQuiz posts answers & returns score", async () => {
    mock.onPost("/quiz/submit").reply(200, { score: 80, nextLevel: "hard" });
    const res = await submitQuiz({ answers: [] });
    expect(res.nextLevel).toBe("hard");
  });
});
