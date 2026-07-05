package com.elmotamyez.gallery.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

const val SUPABASE_URL = "https://ekcpmpkudcyuqcihzzqw.supabase.co"
const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVrY3BtcGt1ZGN5dXFjaWh6enF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODA3ODk3NjEsImV4cCI6MjA5NjM2NTc2MX0.Qx1FvuQ_6rxP51gpQtZeCNAJRmjnU5GXL72995Y6NmI"

val supabaseClient = createSupabaseClient(
    supabaseUrl = SUPABASE_URL,
    supabaseKey = SUPABASE_KEY
) {
    install(Postgrest)
}
