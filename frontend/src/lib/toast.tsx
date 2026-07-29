"use client";

import React, { createContext, useContext, useEffect, useMemo, useReducer } from "react";

export type ToastType = "success" | "error" | "info";

export type ToastItem = {
  id: string;
  type: ToastType;
  message: string;
};

type ToastState = {
  toasts: ToastItem[];
};

type ToastAction =
  | { type: "add"; toast: ToastItem }
  | { type: "remove"; id: string }
  | { type: "clear" };

type ToastContextValue = {
  toasts: ToastItem[];
  addToast: (toast: Omit<ToastItem, "id">) => string;
  removeToast: (id: string) => void;
};

type ToastApi = {
  success: (message: string) => string | undefined;
  error: (message: string) => string | undefined;
  info: (message: string) => string | undefined;
};

const ToastContext = createContext<ToastContextValue | null>(null);

let externalAddToast: ToastContextValue["addToast"] | null = null;
let externalRemoveToast: ToastContextValue["removeToast"] | null = null;

function createToastId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `toast-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function reducer(state: ToastState, action: ToastAction): ToastState {
  switch (action.type) {
    case "add":
      return { toasts: [...state.toasts, action.toast].slice(-5) };
    case "remove":
      return { toasts: state.toasts.filter((toast) => toast.id !== action.id) };
    case "clear":
      return { toasts: [] };
    default:
      return state;
  }
}

function useToastContext() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [state, dispatch] = useReducer(reducer, { toasts: [] });

  const addToast = (toast: Omit<ToastItem, "id">) => {
    const id = createToastId();
    dispatch({ type: "add", toast: { ...toast, id } });
    return id;
  };

  const removeToast = (id: string) => {
    dispatch({ type: "remove", id });
  };

  useEffect(() => {
    externalAddToast = addToast;
    externalRemoveToast = removeToast;
    return () => {
      if (externalAddToast === addToast) externalAddToast = null;
      if (externalRemoveToast === removeToast) externalRemoveToast = null;
    };
  }, [addToast]);

  const value = useMemo<ToastContextValue>(() => ({ toasts: state.toasts, addToast, removeToast }), [state.toasts]);

  return <ToastContext.Provider value={value}>{children}</ToastContext.Provider>;
}

export function useToast() {
  const { addToast } = useToastContext();

  return useMemo<ToastApi>(
    () => ({
      success: (message: string) => addToast({ type: "success", message }),
      error: (message: string) => addToast({ type: "error", message }),
      info: (message: string) => addToast({ type: "info", message }),
    }),
    [addToast]
  );
}

export const toast: ToastApi = {
  success(message: string) {
    return externalAddToast?.({ type: "success", message });
  },
  error(message: string) {
    return externalAddToast?.({ type: "error", message });
  },
  info(message: string) {
    return externalAddToast?.({ type: "info", message });
  },
};

export function useToastActions() {
  return useToastContext();
}
