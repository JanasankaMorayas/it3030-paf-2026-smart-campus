export default function RoleGate({ allow, fallback = null, children }) {
  return allow ? children : fallback;
}
