/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        primary: "#0F172A",
        secondary: "#334155",
        cta: "#0369A1",
        "cta-hover": "#075985",
        surface: "#F8FAFC",
        text: "#020617",
        muted: "#475569",
        border: "#E2E8F0",
        success: "#15803D",
        danger: "#B91C1C",
      },
      fontFamily: {
        sans: ["Plus Jakarta Sans", "system-ui", "sans-serif"],
      },
      borderRadius: {
        md: "8px",
        lg: "12px",
      },
    },
  },
  plugins: [],
};
