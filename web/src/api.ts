import {
  UserProfile,
  NearbyUserCard,
  ConnectionRequestItem,
  MutualConnectionItem,
  InstagramVerificationResult
} from './types';

const API_BASE = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
  ? ''
  : '';

export async function verifyInstagram(username: string): Promise<InstagramVerificationResult> {
  const clean = username.trim().replace(/^@/, '').toLowerCase();
  const res = await fetch(`${API_BASE}/api/v1/users/verify-instagram/${encodeURIComponent(clean)}`);
  if (!res.ok) {
    throw new Error(`Failed to verify Instagram handle (HTTP ${res.status})`);
  }
  return res.json();
}

export async function registerUser(payload: {
  display_name: string;
  sync_username?: string;
  instagram_username: string;
  bio?: string;
  location?: string;
  avatar_url?: string;
}): Promise<UserProfile> {
  const sync_username = payload.sync_username || payload.instagram_username;
  const bodyPayload = { ...payload, sync_username };
  const res = await fetch(`${API_BASE}/api/v1/users/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(bodyPayload)
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.detail || `Registration failed (HTTP ${res.status})`);
  }
  return res.json();
}

export async function getUserProfile(userId: string): Promise<UserProfile> {
  const res = await fetch(`${API_BASE}/api/v1/users/${encodeURIComponent(userId)}`);
  if (!res.ok) {
    throw new Error(`User not found (HTTP ${res.status})`);
  }
  return res.json();
}

export async function pingLocation(
  userId: string,
  latitude: number,
  longitude: number,
  displayName?: string,
  avatarUrl?: string
): Promise<void> {
  await fetch(`${API_BASE}/api/v1/radar/ping`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      user_id: userId,
      latitude,
      longitude,
      display_name: displayName,
      avatar_url: avatarUrl
    })
  }).catch(() => {});
}

export async function getNearbyUsers(
  userId: string,
  latitude?: number,
  longitude?: number,
  radiusMeters = 50000
): Promise<NearbyUserCard[]> {
  const params = new URLSearchParams({
    user_id: userId,
    max_distance: String(radiusMeters),
    radius: String(radiusMeters)
  });
  if (latitude !== undefined && longitude !== undefined) {
    params.set('lat', String(latitude));
    params.set('lng', String(longitude));
  }
  const url = `${API_BASE}/api/v1/radar/nearby?${params.toString()}`;
  try {
    const res = await fetch(url);
    if (!res.ok) return [];
    const data = await res.json();
    if (!Array.isArray(data)) return [];

    return data.map((p: any) => {
      const d = Number(p.distance_meters ?? p.approx_distance_meters ?? 5.0);
      const isPending = p.is_connection_pending || p.connection_status === 'REQUEST_SENT' || p.connection_status === 'PENDING';
      const isConnected = p.connection_status === 'ACCEPTED';
      return {
        user_id: p.user_id,
        display_name: p.display_name || 'Nearby User',
        bio: p.bio || '',
        location: p.location || p.location_name || 'Nearby',
        location_name: p.location_name || p.location || 'Nearby',
        avatar_url: p.avatar_url || '',
        distance_meters: isNaN(d) ? 5.0 : d,
        approx_distance_meters: isNaN(d) ? 5.0 : d,
        confidence: p.confidence || (d <= 15 ? 'HIGH' : (d <= 100 ? 'MEDIUM' : 'LOW')),
        interests: Array.isArray(p.interests) ? p.interests : [],
        is_connection_pending: isPending,
        connection_status: p.connection_status || (isConnected ? 'ACCEPTED' : (isPending ? 'REQUEST_SENT' : 'NONE')),
        instagram_username: p.instagram_username || null
      };
    });
  } catch {
    return [];
  }
}

export async function sendConnectionRequest(senderId: string, receiverId: string): Promise<boolean> {
  const res = await fetch(`${API_BASE}/api/v1/connections/request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sender_id: senderId, receiver_id: receiverId })
  });
  return res.ok;
}

export async function getPendingRequests(userId: string): Promise<ConnectionRequestItem[]> {
  const res = await fetch(`${API_BASE}/api/v1/connections/pending/${encodeURIComponent(userId)}`);
  if (!res.ok) return [];
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

export async function respondConnectionRequest(requestId: string, accept: boolean): Promise<boolean> {
  const action = accept ? 'accept' : 'reject';
  const res = await fetch(`${API_BASE}/api/v1/connections/${action}/${encodeURIComponent(requestId)}`, {
    method: 'POST'
  });
  return res.ok;
}

export async function getMutualConnections(userId: string): Promise<MutualConnectionItem[]> {
  const res = await fetch(`${API_BASE}/api/v1/connections/mutual/${encodeURIComponent(userId)}`);
  if (!res.ok) return [];
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

export async function toggleDiscovery(userId: string, isActive: boolean): Promise<boolean> {
  const res = await fetch(`${API_BASE}/api/v1/users/${encodeURIComponent(userId)}/discovery?is_active=${isActive}`, {
    method: 'PATCH'
  });
  return res.ok;
}

export async function updateProfile(userIdOrPayload: string | any, payload?: any): Promise<UserProfile> {
  const actualPayload = payload || userIdOrPayload;
  return registerUser(actualPayload);
}
export const verifyInstagramAccount = verifyInstagram;
