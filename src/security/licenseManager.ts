export const SHARED_SECRET_2D = 'LUCKY2D_SHWEKHINMYAY_OFFLINE_SECRET_SALT_2026';
export const SHARED_SECRET = 'Lucky3D_Super_Secret_Key_2026!#';

const KEY_ACTIVATED = 'lucky2d_is_activated';
const KEY_ACTIVATED_KEY = 'lucky2d_activated_product_key';
const KEY_DEVICE_ID = 'lucky2d_device_id';

export function getDeviceId(): string {
  let id = localStorage.getItem(KEY_DEVICE_ID);
  if (!id) {
    const chars = '0123456789ABCDEF';
    let rand = '';
    for (let i = 0; i < 16; i++) {
      rand += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    id = rand;
    localStorage.setItem(KEY_DEVICE_ID, id);
  }
  return id;
}

export function normalizeKeyInput(raw: string): string {
  return raw
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 12);
}

export function formatKeyWithDashes(raw: string): string {
  const norm = normalizeKeyInput(raw);
  const parts: string[] = [];
  for (let i = 0; i < norm.length; i += 4) {
    parts.push(norm.slice(i, i + 4));
  }
  return parts.join('-');
}

async function computeHmacSha256Hex(payload: string, secret: string): Promise<string> {
  const enc = new TextEncoder();
  const key = await crypto.subtle.importKey(
    'raw',
    enc.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  );
  const sig = await crypto.subtle.sign('HMAC', key, enc.encode(payload));
  const hashArray = Array.from(new Uint8Array(sig));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('').toUpperCase();
}

async function computeSha256Hex(payload: string): Promise<string> {
  const enc = new TextEncoder();
  const digest = await crypto.subtle.digest('SHA-256', enc.encode(payload));
  const hashArray = Array.from(new Uint8Array(digest));
  return hashArray.map((b) => b.toString(16).padStart(2, '0')).join('').toUpperCase();
}

export async function generateExpectedKey(deviceId: string, secret = SHARED_SECRET_2D): Promise<string> {
  const hex = await computeHmacSha256Hex(deviceId.trim().toUpperCase(), secret);
  return formatKeyWithDashes(hex.slice(0, 12));
}

export async function verifyAndSaveLicense(inputKey: string): Promise<boolean> {
  const currentDeviceId = getDeviceId();
  const normalizedInput = normalizeKeyInput(inputKey);
  if (normalizedInput.length !== 12) return false;

  const devUpper = currentDeviceId.toUpperCase();
  const devLower = currentDeviceId.toLowerCase();

  const candidates: string[] = [];

  // 1. HMAC-SHA256 with 2D Secret Salt (Upper & Lower)
  candidates.push(normalizeKeyInput((await computeHmacSha256Hex(devUpper, SHARED_SECRET_2D)).slice(0, 12)));
  candidates.push(normalizeKeyInput((await computeHmacSha256Hex(devLower, SHARED_SECRET_2D)).slice(0, 12)));

  // 2. SHA-256(deviceId + salt) & SHA-256(salt + deviceId)
  candidates.push(normalizeKeyInput((await computeSha256Hex(devUpper + SHARED_SECRET_2D)).slice(0, 12)));
  candidates.push(normalizeKeyInput((await computeSha256Hex(devLower + SHARED_SECRET_2D)).slice(0, 12)));
  candidates.push(normalizeKeyInput((await computeSha256Hex(SHARED_SECRET_2D + devUpper)).slice(0, 12)));
  candidates.push(normalizeKeyInput((await computeSha256Hex(SHARED_SECRET_2D + devLower)).slice(0, 12)));

  // 3. Legacy Secret compatibility
  candidates.push(normalizeKeyInput((await computeHmacSha256Hex(devUpper, SHARED_SECRET)).slice(0, 12)));
  candidates.push(normalizeKeyInput((await computeHmacSha256Hex(devLower, SHARED_SECRET)).slice(0, 12)));

  const isValid = candidates.some((c) => c === normalizedInput);

  if (isValid) {
    localStorage.setItem(KEY_ACTIVATED, 'true');
    localStorage.setItem(KEY_ACTIVATED_KEY, formatKeyWithDashes(normalizedInput));
  }
  return isValid;
}

export function isAppActivated(): boolean {
  return localStorage.getItem(KEY_ACTIVATED) === 'true';
}

export function setAppActivated(activated: boolean): void {
  if (activated) {
    localStorage.setItem(KEY_ACTIVATED, 'true');
  } else {
    localStorage.removeItem(KEY_ACTIVATED);
    localStorage.removeItem(KEY_ACTIVATED_KEY);
  }
}
