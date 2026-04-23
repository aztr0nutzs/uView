package com.sentinel.app.domain.repository

import com.sentinel.app.domain.model.CameraDevice
import com.sentinel.app.domain.model.CameraEvent
import com.sentinel.app.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// CameraRepository
// Primary persistence interface for camera device configuration.
// ─────────────────────────────────────────────────────────────────────────────

interface CameraRepository {

    /** Observe all cameras as a reactive stream. */
    fun observeAllCameras(): Flow<List<CameraDevice>>

    /** Observe cameras for a specific room. */
    fun observeCamerasByRoom(room: String): Flow<List<CameraDevice>>

    /** Observe favorite cameras. */
    fun observeFavoriteCameras(): Flow<List<CameraDevice>>

    /** Get a single camera by ID — returns null if not found. */
    suspend fun getCameraById(id: String): CameraDevice?

    /** Insert or replace a camera configuration. */
    suspend fun saveCamera(camera: CameraDevice)

    /** Update only the enabled/disabled state. */
    suspend fun setCameraEnabled(cameraId: String, enabled: Boolean)

    /** Toggle favorite state. Returns new state. */
    suspend fun toggleFavorite(cameraId: String): Boolean

    /** Toggle pinned state. Returns new state. */
    suspend fun togglePinned(cameraId: String): Boolean

    /** Rename a camera. */
    suspend fun renameCamera(cameraId: String, newName: String)

    /** Move camera to a different room. */
    suspend fun assignRoom(cameraId: String, room: String)

    /** Permanently delete a camera configuration. */
    suspend fun deleteCamera(cameraId: String)

    /** Get all distinct room names. */
    suspend fun getAllRooms(): List<String>

    /** Get a live dashboard summary count. */
    fun observeDashboardSummary(): Flow<DashboardSummary>
}

// ─────────────────────────────────────────────────────────────────────────────
// CameraEventRepository
// Persistence for camera events (motion, connection changes, etc.)
// ─────────────────────────────────────────────────────────────────────────────

interface CameraEventRepository {

    /** Observe all events, newest first. */
    fun observeEvents(limit: Int = 200): Flow<List<CameraEvent>>

    /** Observe events for a specific camera. */
    fun observeEventsForCamera(cameraId: String, limit: Int = 50): Flow<List<CameraEvent>>

    /** Count unread events. */
    fun observeUnreadCount(): Flow<Int>

    /** Insert a new event. */
    suspend fun addEvent(event: CameraEvent)

    /** Mark event(s) as read. */
    suspend fun markAsRead(eventIds: List<String>)

    /** Mark all events as read. */
    suspend fun markAllAsRead()

    /** Delete events older than a threshold. */
    suspend fun pruneOldEvents(olderThanMs: Long)

    /** Delete all events for a camera (called when camera is removed). */
    suspend fun deleteEventsForCamera(cameraId: String)
}
