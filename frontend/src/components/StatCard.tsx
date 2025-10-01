type Props = {
  label: string;
  value: string;
  sub?: string;
};

export default function StatCard({ label, value, sub }: Props) {
  return (
    <div className="card">
      <div className="label">{label}</div>
      <div className="value">{value}</div>
      {sub && <div className="subtle" style={{ marginTop: 6 }}>{sub}</div>}
    </div>
  );
}
