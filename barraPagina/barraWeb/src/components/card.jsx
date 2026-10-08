export default function Card({ children, className = "" }) {
  return (
    <div className={`rounded-lg border border-border bg-white p-6 ${className}`}>
      {children}
    </div>
  );
}
