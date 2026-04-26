export const PAGE_SIZE_OPTIONS = [5, 10, 20, 50];

export const RESOURCE_TYPES = ["LECTURE_HALL", "LAB", "MEETING_ROOM", "EQUIPMENT"];
export const RESOURCE_STATUSES = ["ACTIVE", "OUT_OF_SERVICE"];

export const BOOKING_STATUSES = ["PENDING", "APPROVED", "REJECTED", "CANCELLED"];
export const BOOKING_SORT_OPTIONS = [
  { value: "startTime,asc", label: "Start time (earliest first)" },
  { value: "createdAt,desc", label: "Newest requests" },
  { value: "status,asc", label: "Status" },
];

export const TICKET_STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "CANCELLED"];
export const TICKET_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
export const TICKET_CATEGORIES = ["ELECTRICAL", "PLUMBING", "NETWORK", "CLEANING", "SAFETY", "OTHER"];
export const TICKET_SORT_OPTIONS = [
  { value: "createdAt,desc", label: "Newest tickets" },
  { value: "priority,asc", label: "Priority" },
  { value: "status,asc", label: "Status" },
];

export const NOTIFICATION_TYPES = [
  "BOOKING_CREATED",
  "BOOKING_APPROVED",
  "BOOKING_REJECTED",
  "BOOKING_CANCELLED",
  "TICKET_CREATED",
  "TICKET_ASSIGNED",
  "TICKET_STATUS_UPDATED",
  "TICKET_RESOLVED",
  "GENERAL",
];
export const NOTIFICATION_SORT_OPTIONS = [
  { value: "createdAt,desc", label: "Newest first" },
  { value: "type,asc", label: "Type" },
  { value: "readAt,desc", label: "Recently read" },
];

export const USER_ROLES = ["USER", "TECHNICIAN", "ADMIN"];

export const AUDIT_ENTITY_TYPES = ["BOOKING", "TICKET", "USER", "NOTIFICATION", "BACKFILL"];
export const AUDIT_SORT_OPTIONS = [
  { value: "createdAt,desc", label: "Latest activity" },
  { value: "entityType,asc", label: "Entity type" },
  { value: "action,asc", label: "Action" },
];
