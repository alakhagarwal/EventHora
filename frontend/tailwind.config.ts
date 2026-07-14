import type { Config } from "tailwindcss";
const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        navy: { DEFAULT: "#0f1b3d", 700: "#1e3a5f", 500: "#3b6fa0" },
        gold: { DEFAULT: "#c9a84c", 300: "#e8c968", 600: "#a88a2f" },
        cream: "#faf7f0",
        ink: "#0b1220",
      },
      fontFamily: {
        display: ["Playfair Display", "Georgia", "serif"],
        sans: ["Inter", "ui-sans-serif", "system-ui"],
      },
      boxShadow: { soft: "0 10px 30px -12px rgba(15,27,61,0.18)" },
    },
  },
  plugins: [],
};
export default config;
