const VARIANTES = {
  primary: "bg-cta text-white hover:bg-cta-hover",
  secondary: "bg-white text-primary border border-border hover:bg-surface",
  disabled: "bg-border text-muted cursor-not-allowed",
};

export default function Button({
  children,
  variant = "primary",
  disabled,
  className = "",
  as: Component = "button",
  ...props
}) {
  const estilo = disabled ? VARIANTES.disabled : VARIANTES[variant];
  const esBotonNativo = Component === "button";

  return (
    <Component
      {...(esBotonNativo ? { disabled, type: props.type || "button" } : {})}
      className={`inline-flex items-center justify-center rounded-md px-5 py-2.5 text-sm font-semibold transition-colors duration-200 ${
        disabled ? "" : "cursor-pointer"
      } ${estilo} ${className}`}
      {...props}
    >
      {children}
    </Component>
  );
}
