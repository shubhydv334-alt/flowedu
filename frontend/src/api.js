import { useCallback, useEffect, useState } from "react";

const KEY = "flowedu_auth";

export const getAuth = () => {
  try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; }
};
export const setAuth = (a) => (a ? localStorage.setItem(KEY, JSON.stringify(a)) : localStorage.removeItem(KEY));

/** Calls /api/... with the JWT. Throws Error(message) with the server's error text. */
export async function api(path, { method = "GET", body, form } = {}) {
  const headers = {};
  const auth = getAuth();
  if (auth?.token) headers.Authorization = `Bearer ${auth.token}`;
  let payload;
  if (form) payload = form;
  else if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    payload = JSON.stringify(body);
  }
  let res;
  try {
    res = await fetch(`/api${path}`, { method, headers, body: payload });
  } catch {
    throw new Error("Cannot reach the server. Check your connection and try again.");
  }
  if (res.status === 401 && auth) {
    setAuth(null);
    window.location.href = "/login";
    throw new Error("Session expired, please sign in again");
  }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) throw new Error((data && data.error) || `Request failed (${res.status})`);
  return data;
}

/** GET hook: { data, error, loading, reload }. Pass null to skip. */
export function useApi(path) {
  const [state, setState] = useState({ data: null, error: null, loading: !!path });
  const load = useCallback(() => {
    if (!path) return Promise.resolve();
    setState((s) => ({ ...s, loading: true }));
    return api(path)
      .then((data) => setState({ data, error: null, loading: false }))
      .catch((e) => setState({ data: null, error: e.message, loading: false }));
  }, [path]);
  useEffect(() => { load(); }, [load]);
  return { ...state, reload: load };
}
