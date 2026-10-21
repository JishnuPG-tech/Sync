-- ====================================================================
-- SYNC APP - SUPABASE DATABASE SCHEMA INITIALIZATION
-- Paste this entire script into your Supabase SQL Editor and click RUN
-- ====================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS public.users (
    id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    sync_username TEXT UNIQUE,
    instagram_username TEXT UNIQUE NOT NULL,
    bio TEXT DEFAULT '',
    location_name TEXT DEFAULT '',
    avatar_url TEXT,
    ble_token TEXT UNIQUE,
    interests_csv TEXT DEFAULT '',
    is_discovery_active BOOLEAN DEFAULT TRUE,
    fcm_token TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. User Locations Table (for Proximity Radar)
CREATE TABLE IF NOT EXISTS public.user_locations (
    user_id TEXT PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Connection Requests Table (for Mutual Reveals)
CREATE TABLE IF NOT EXISTS public.connection_requests (
    id TEXT PRIMARY KEY,
    sender_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    receiver_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    responded_at TIMESTAMPTZ
);

-- 4. Fast Spatial & Status Indexes
CREATE INDEX IF NOT EXISTS idx_locations_lat_lng ON public.user_locations(latitude, longitude);
CREATE INDEX IF NOT EXISTS idx_conn_sender_receiver ON public.connection_requests(sender_id, receiver_id);
CREATE INDEX IF NOT EXISTS idx_conn_status ON public.connection_requests(status);

-- 5. Enable Row Level Security (RLS) - backend operates with service_role which bypasses RLS
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.connection_requests ENABLE ROW LEVEL SECURITY;

-- 6. Create permissive read policies for authenticated/service role
CREATE POLICY "Allow public read users" ON public.users FOR SELECT USING (true);
CREATE POLICY "Allow public read locations" ON public.user_locations FOR SELECT USING (true);
CREATE POLICY "Allow public read connections" ON public.connection_requests FOR SELECT USING (true);
