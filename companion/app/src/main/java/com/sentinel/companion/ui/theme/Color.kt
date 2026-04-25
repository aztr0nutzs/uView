package com.sentinel.companion.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary — Orange ────────────────────────────────────────────────────────
val OrangePrimary   = Color(0xFFFF6B00)
val OrangeSoft      = Color(0xFFFF9159)
val OrangeDim       = Color(0xFFFF7524)
val OrangeContainer = Color(0xFFFF7A2F)
val OrangeSubtle    = Color(0x1AFF6B00)
val OrangeGlow      = Color(0x66FF6B00)

// ─── Secondary — Green (Online/Active) ───────────────────────────────────────
val GreenOnline     = Color(0xFF32FF00)
val GreenDim        = Color(0xFF2EEF00)
val GreenContainer  = Color(0xFF116E00)
val GreenGlow       = Color(0x4032FF00)

// ─── Tertiary — Cyan (Stream data, links) ────────────────────────────────────
val CyanTertiary    = Color(0xFF9CF7FF)
val CyanTertiaryDim = Color(0xFF00E1EF)
val CyanFixed       = Color(0xFF12F1FF)
val CyanAccent      = Color(0xFF00EEFC)
val CyanSubtleBg    = Color(0x1A00E1EF)

// ─── Error — Red ─────────────────────────────────────────────────────────────
val ErrorRed        = Color(0xFFFF7351)
val ErrorContainer  = Color(0xFFB92902)
val ErrorDim        = Color(0xFFD53D18)
val ErrorGlow       = Color(0x40FF7351)

// ─── Status ──────────────────────────────────────────────────────────────────
val StatusOnline     = GreenOnline
val StatusOffline    = ErrorRed
val StatusConnecting = Color(0xFFFFAB40)
val StatusDisabled   = Color(0xFF616161)
val StatusUnknown    = Color(0xFF757575)
val RecordingRed     = ErrorRed

// ─── Backgrounds (layered) ───────────────────────────────────────────────────
val BackgroundDeep  = Color(0xFF0E0E0E)
val SurfaceLowest   = Color(0xFF000000)
val SurfaceLow      = Color(0xFF131313)
val SurfaceBase     = Color(0xFF1A1A1A)
val SurfaceElevated = Color(0xFF20201F)
val SurfaceHighest  = Color(0xFF262626)
val SurfaceBright   = Color(0xFF2C2C2C)

// ─── Borders ─────────────────────────────────────────────────────────────────
val SurfaceStroke   = Color(0xFF484847)
val OutlineColor    = Color(0xFF767575)

// ─── Text ────────────────────────────────────────────────────────────────────
val TextPrimary     = Color(0xFFFFFFFF)
val TextSecondary   = Color(0xFFADAAAA)
val TextDisabled    = Color(0xFF767575)
val TextOnOrange    = Color(0xFF531E00)

// ─── Feed overlays ───────────────────────────────────────────────────────────
val FeedBorderActive = OrangePrimary
val FeedScrim        = Color(0xCC000000)
