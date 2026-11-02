export interface UserProfile {
  id: string;
  display_name: string;
  sync_username?: string;
  instagram_username: string;
  bio?: string;
  location?: string;
  avatar_url?: string;
  ble_token?: string;
  interests?: string[];
  is_discovery_active?: boolean;
  created_at?: string;
}

export interface NearbyUserCard {
  user_id: string;
  display_name: string;
  bio: string;
  location?: string;
  location_name: string;
  avatar_url?: string;
  distance_meters: number;
  approx_distance_meters?: number;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
  interests: string[];
  is_connection_pending?: boolean;
  connection_status?: string;
  instagram_username?: string | null;
}

export interface ConnectionRequestItem {
  id: string;
  sender_id: string;
  receiver_id: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  created_at: string;
  sender_profile?: {
    id: string;
    display_name: string;
    avatar_url?: string;
    bio?: string;
    location?: string;
  };
  receiver_profile?: {
    id: string;
    display_name: string;
    avatar_url?: string;
    bio?: string;
    location?: string;
  };
}

export interface MutualConnectionItem {
  id: string;
  user_id: string;
  display_name: string;
  instagram_username: string;
  avatar_url?: string;
  bio?: string;
  matched_at: string;
}

export interface InstagramVerificationResult {
  exists: boolean;
  username: string;
  full_name?: string;
  avatar_url?: string;
  description?: string;
  is_private?: boolean;
  error?: string | null;
}
