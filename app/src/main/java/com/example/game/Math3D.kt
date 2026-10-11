package com.example.game

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lengthSquared(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 0.0001f) this / len else Vec3(0f, 1f, 0f)
    }

    fun distanceTo(other: Vec3): Float = (this - other).length()

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val FORWARD = Vec3(0f, 0f, 1f)
        val RIGHT = Vec3(1f, 0f, 0f)

        fun lerp(a: Vec3, b: Vec3, t: Float): Vec3 {
            val clampedT = t.coerceIn(0f, 1f)
            return a + (b - a) * clampedT
        }
    }
}

data class ProjectedPoint(
    val x: Float,
    val y: Float,
    val depth: Float,
    val isVisible: Boolean
)

class Camera3D(
    var position: Vec3 = Vec3(0f, 5f, -8f),
    var target: Vec3 = Vec3(0f, 1f, 8f),
    var fovDegrees: Float = 60f,
    var near: Float = 0.5f,
    var far: Float = 140f
) {
    private var forward = Vec3.FORWARD
    private var right = Vec3.RIGHT
    private var up = Vec3.UP

    fun updateVectors() {
        forward = (target - position).normalized()
        val worldUp = Vec3.UP
        right = worldUp.cross(forward).normalized()
        up = forward.cross(right).normalized()
    }

    fun project(worldPoint: Vec3, screenWidth: Float, screenHeight: Float): ProjectedPoint? {
        val d = worldPoint - position
        val cz = d.dot(forward)

        if (cz <= near || cz >= far) {
            return null
        }

        val cx = d.dot(right)
        val cy = d.dot(up)

        val tanHalfFov = tan(Math.toRadians((fovDegrees / 2.0)).toFloat())
        val scale = (screenHeight * 0.5f) / tanHalfFov

        val sx = (screenWidth * 0.5f) + (cx / cz) * scale
        val sy = (screenHeight * 0.5f) - (cy / cz) * scale

        val margin = 80f
        val isVisible = sx >= -margin && sx <= screenWidth + margin &&
                sy >= -margin && sy <= screenHeight + margin

        return ProjectedPoint(sx, sy, cz, isVisible)
    }
}
