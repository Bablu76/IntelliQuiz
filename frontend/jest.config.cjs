module.exports = {
  testEnvironment: "jsdom",
  moduleFileExtensions: ["js", "jsx"],
  transform: {
    "^.+\\.[t|j]sx?$": "babel-jest"
  },
  setupFilesAfterEnv: ["C:/Shared/Projects/IntelliQuiz/frontend/src/setupTests.js"],
  moduleNameMapper: {
    "\\.(css|less|scss|sass)$": "identity-obj-proxy"
  }
};
