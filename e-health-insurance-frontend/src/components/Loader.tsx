export function Loader({ label }: { label?: string }) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: 12,
        padding: 48,
        color: "var(--text-secondary)",
      }}
    >
      <span className="spinner spinner-lg" />
      {label && <span>{label}</span>}
    </div>
  );
}

export function InlineLoader() {
  return <span className="spinner" />;
}
