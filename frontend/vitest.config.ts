import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
      "next/navigation": path.resolve(__dirname, "./src/__tests__/mocks/next-navigation.ts"),
      "next/link": path.resolve(__dirname, "./src/__tests__/mocks/next-link.ts"),
    },
  },
  test: {
    environment: "jsdom",
    environmentOptions: {
      jsdom: {
        url: "http://localhost",
      },
    },
    setupFiles: ["./src/__tests__/setup.ts"],
    globals: true,
    css: false,
  },
});
