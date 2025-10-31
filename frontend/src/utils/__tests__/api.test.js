import axios from "axios";
import MockAdapter from "axios-mock-adapter";
import { jest } from "@jest/globals";     // ✅ ESM-safe Jest import
import api from "../api";


describe("api.js", () => {
  const mock = new MockAdapter(axios);
  beforeEach(() => mock.reset());

  test("adds Authorization header when token exists", async () => {
    localStorage.setItem("token", "abc123");
    mock.onGet("/ping").reply(200, { ok: true });
    const res = await api.get("/ping");
    expect(res.config.headers.Authorization).toBe("Bearer abc123");
  });

  test("handles 401 → redirects to /login", async () => {
    Object.defineProperty(window, "location", {
      value: { assign: jest.fn() },
      writable: true,
    });
    mock.onGet("/secure").reply(401);
    await expect(api.get("/secure")).rejects.toThrow();
    expect(window.location.assign).toHaveBeenCalledWith("/login");
  });
});
