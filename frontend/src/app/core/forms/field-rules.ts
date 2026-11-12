export const EMAIL_PATTERN = /^[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$/i;
export const DNI_PATTERN = /^[0-9]{8}$/;
export const PERU_MOBILE_PATTERN = /^[0-9]{9}$/;

export function hasValidOptionalPeruMobile(value: string | null | undefined): boolean {
  const normalized = value?.trim() ?? '';
  return normalized.length === 0 || PERU_MOBILE_PATTERN.test(normalized);
}

export function hasValidEmail(value: string | null | undefined): boolean {
  const normalized = value?.trim() ?? '';
  return normalized.length > 0 && normalized.length <= 254 && EMAIL_PATTERN.test(normalized);
}

export function hasValidOptionalLength(value: string | null | undefined, max: number): boolean {
  return (value?.trim().length ?? 0) <= max;
}

export function isValidIsoDate(value: string | null | undefined): boolean {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  const candidate = new Date(Date.UTC(year, month - 1, day));
  return candidate.getUTCFullYear() === year
    && candidate.getUTCMonth() === month - 1
    && candidate.getUTCDate() === day;
}
