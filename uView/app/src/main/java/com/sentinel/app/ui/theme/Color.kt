package com.sentinel.app.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Sentinel Design System — Color Tokens
// Updated in Phase 7/8 to match the tactical UI design from the HTML screens.
//
// Primary accent: Orange (#FF6B00) — calls-to-action, active states, headers
// Secondary:      Green (#32FF00)  — online status, success, active feeds
// Tertiary:       Cyan  (#00E1EF)  — stream data, performance metrics, links
// Error:          Red   (#FF7351)  — offline, critical, recording indicator
// ─────────────────────────────────────────────────────────────────────────────

// ── Primary — Orange ─────────────────────────────────────────────────────────
val OrangePrimary      = Color(0xFFFF6B00)   // main CTA, active nav, header text
val OrangeSoft         = Color(0xFFFF9159)   // HTML primary token
val OrangeDim          = Color(0xFFFF7524)   // pressed state
val OrangeContainer    = Color(0xFFFF7A2F)   // button fill
val OrangeSubtle       = Color(0x1AFF6B00)   // tinted surface (10% alpha)
val OrangeGlow         = Color(0x66FF6B00)   // shadow/glow effect (40% alpha)

// ── Secondary — Green (Online/Active) ────────────────────────────────────────
val GreenOnline        = Color(0xFF32FF00)   // online dot, active feed indicator
val GreenDim           = Color(0xFF2EEF00)   // slightly dimmer green
val GreenContainer     = Color(0xFF116E00)   // green chip background
val GreenGlow          = Color(0x4032FF00)   // green glow (25% alpha)

// ── Tertiary — Cyan (Stream data, links) ─────────────────────────────────────
val CyanTertiary       = Color(0xFF9CF7FF)   // light cyan text
val CyanTertiaryDim    = Color(0xFF00E1EF)   // cyan stream metrics
val CyanFixed          = Color(0xFF12F1FF)   // cyan fixed elements
val CyanAccent         = Color(0xFF00EEFC)   // link/interactive cyan
val CyanSubtleBg       = Color(0x1A00E1EF)   // cyan tinted surface
val CyanSubtle         = CyanSubtleBg

// ── Error — Red ──────────────────────────────────────────────────────────────
val ErrorRed           = Color(0xFFFF7351)   // offline status, errors
val ErrorContainer     = Color(0xFFB92902)   // error card background
val ErrorDim           = Color(0xFFD53D18)   // dimmer error state
val ErrorGlow          = Color(0x40FF7351)   // error glow

// ── Status (legacy names kept for compatibility) ──────────────────────────────
val StatusOnline       = GreenOnline
val StatusOffline      = ErrorRed
val StatusConnecting   = Color(0xFFFFAB40)   // amber — in-progress
val StatusDisabled     = Color(0xFF616161)
val StatusUnknown      = Color(0xFF757575)

// ── Recording indicator ───────────────────────────────────────────────────────
val RecordingRed       = ErrorRed

// ── Backgrounds (layered) ─────────────────────────────────────────────────────
val BackgroundDeep     = Color(0xFF0E0E0E)   // true app background (HTML: surface-dim)
val SurfaceLowest      = Color(0xFF000000)   // darkest surface (HTML: surface-container-lowest)
val SurfaceLow         = Color(0xFF131313)   // low surface (HTML: surface-container-low)
val SurfaceBase        = Color(0xFF1A1A1A)   // card surface (HTML: surface-container)
val SurfaceElevated    = Color(0xFF20201F)   // raised surface (HTML: surface-container-high)
val SurfaceHighest     = Color(0xFF262626)   // top surface (HTML: surface-container-highest)
val SurfaceBright      = Color(0xFF2C2C2C)   // brightest surface

// ── Borders ───────────────────────────────────────────────────────────────────
val SurfaceStroke      = Color(0xFF484847)   // outline-variant — card borders, dividers
val OutlineColor       = Color(0xFF767575)   // outline — labels, disabled text

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary        = Color(0xFFFFFFFF)   // on-surface
val TextSecondary      = Color(0xFFADAAAA)   // on-surface-variant
val TextDisabled       = Color(0xFF767575)   // outline
val TextOnOrange       = Color(0xFF531E00)   // on-primary (dark text on orange)

// ── Feed/tile overlays ────────────────────────────────────────────────────────
val FeedBorder         = SurfaceHighest
val FeedBorderActive   = OrangePrimary
val FeedScrim          = Color(0xCC000000)

// Keep legacy aliases so existing code compiles
val CyanPrimary        = CyanAccent         // brighter tactical cyan used by HTML inactive controls/labels
val WarningAmber       = StatusConnecting
val TealAccent         = GreenDim
