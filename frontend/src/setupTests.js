import "@testing-library/jest-dom";
if (!global.import) global.import = { meta: { env: {} } };
if (!import.meta.env) import.meta.env = {};
import.meta.env.VITE_API_URL = "http://localhost:8080";
