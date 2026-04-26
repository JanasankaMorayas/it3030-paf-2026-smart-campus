export function formatDateTime(value) {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("en-LK", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function formatCompactDateTime(value) {
  if (!value) {
    return "—";
  }

  return new Intl.DateTimeFormat("en-LK", {
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(value));
}

export function formatDateRange(startValue, endValue) {
  if (!startValue || !endValue) {
    return "—";
  }

  return `${formatCompactDateTime(startValue)} - ${formatCompactDateTime(endValue)}`;
}

export function toDateTimeLocalValue(value) {
  if (!value) {
    return "";
  }

  return value.slice(0, 16);
}

export function titleizeEnum(value) {
  if (!value) {
    return "—";
  }

  return String(value)
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function parseMultilineList(value) {
  return value
    .split("\n")
    .map((item) => item.trim())
    .filter(Boolean);
}

export function joinMultilineList(values) {
  return Array.isArray(values) ? values.join("\n") : "";
}
