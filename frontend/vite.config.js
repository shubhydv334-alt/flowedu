import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.{js,jsx}"],
    setupFiles: ["./src/test/setup.js"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.{js,jsx}"],
      exclude: ["src/**/*.test.{js,jsx}", "src/test/**", "src/main.jsx", "src/StyleGuide.jsx"],
      reporter: ["text-summary", "lcov"],
    },
  },
  server: {
    port: 5173,
    proxy: { "/api": "http://localhost:8080" },
  },
});
