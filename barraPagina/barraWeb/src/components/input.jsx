export default function Input({ label, id, error, className = "", ...props }) {
  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label htmlFor={id} className="text-sm font-medium text-primary">
          {label}
        </label>
      )}
      <input
        id={id}
        className={`rounded-md border border-border px-3.5 py-2.5 text-sm text-text placeholder:text-muted focus:border-cta focus:outline-none ${className}`}
        {...props}
      />
      {error && <span className="text-sm text-danger">{error}</span>}
    </div>
  );
}
