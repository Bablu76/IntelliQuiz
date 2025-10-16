import { useState, useEffect, useCallback } from "react";

/**
 * Lightweight JWT decoder (no signature verification) to read exp & roles.
 * Only used client-side to check expiry and read role claims.
 */
function parseJwt(token) {
  if (!token || typeof token !== "string") return null;
  try {
    const base64Url = token.split(".")[1];
    if (!base64Url) return null; // invalid format
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch (err) {
    console.warn("Invalid JWT:", err.message);
    return null;
  }
}

export default function useAuth() {
  const [token, setToken] = useState(() => localStorage.getItem("token"));
  const [roles, setRoles] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("roles") || "[]");
    } catch {
      return [];
    }
  });
  const [userId, setUserId] = useState(() => localStorage.getItem("userId"));
  const [isAuthenticated, setIsAuthenticated] = useState(!!token);

  // ✅ Moved logout to the top so it exists before useEffect uses it
  const logout = useCallback(() => {
    if (window.location.pathname === "/login") return; // prevent duplicate triggers
    console.log("Performing full logout...");

    // Remove all known session keys
    const keysToRemove = [
      "token",
      "roles",
      "userId",
      "username",
      "email",
      "refreshToken",
    ];
    keysToRemove.forEach((key) => localStorage.removeItem(key));

    setToken(null);
    setRoles([]);
    setUserId(null);
    setIsAuthenticated(false);

    console.log("✅ LocalStorage cleared, redirecting...");
    window.location.href = "/login";
  }, []);

  // returns true if token exists and isn't expired
  const tokenValid = useCallback(
    (t = token) => {
      if (!t) return false;
      const claims = parseJwt(t);
      if (!claims || !claims.exp) return false;
      const now = Math.floor(Date.now() / 1000);
      return claims.exp > now - 5; // 5s grace window
    },
    [token]
  );

  useEffect(() => {
    if (token) {
      const valid = tokenValid(token);
      console.log("🔍 Token validity check:", valid);
      if (!valid) {
        console.warn("⚠️ Token invalid on mount — performing logout");
        logout();
      } else {
        setIsAuthenticated(true);
      }
    } else {
      setIsAuthenticated(false);
    }
  }, [token, tokenValid, logout]);

  const saveAuth = useCallback(
    ({ token: newToken, roles: newRoles, userId: newUserId }) => {
      if (newToken) {
        localStorage.setItem("token", newToken);
        setToken(newToken);
      }
      if (newRoles) {
        localStorage.setItem("roles", JSON.stringify(newRoles));
        setRoles(newRoles);
      }
      if (newUserId !== undefined && newUserId !== null) {
        localStorage.setItem("userId", String(newUserId));
        setUserId(String(newUserId));
      }
      setIsAuthenticated(true);
    },
    []
  );

  // Wrapper around fetch that injects Authorization and logs out on 401/403
  const fetchWithAuth = useCallback(
    async (input, init = {}) => {
      try {
        const t = token || localStorage.getItem("token");
        const headers = new Headers(init.headers || {});
        if (t) headers.set("Authorization", `Bearer ${t}`);

        const resp = await fetch(input, { ...init, headers });

        console.log(`[fetchWithAuth] ${resp.status} → ${input}`);

        if (
          (resp.status === 401 || resp.status === 403) &&
          !window.location.pathname.includes("/login")
        ) {
          console.warn("🔒 Unauthorized/Forbidden response — auto-logout triggered");
          logout();
        }

        return resp;
      } catch (err) {
        console.error("🚨 fetchWithAuth failed:", err);
        throw err;
      }
    },
    [token, logout]
  );

  return {
    token,
    roles,
    userId,
    isAuthenticated,
    tokenValid,
    saveAuth,
    logout,
    fetchWithAuth,
    parseJwt,
  };
}
