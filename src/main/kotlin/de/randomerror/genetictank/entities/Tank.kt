package de.randomerror.genetictank.entities

import de.randomerror.genetictank.GameLoop
import de.randomerror.genetictank.helper.rotate
import de.randomerror.genetictank.helper.transformContext
import de.randomerror.genetictank.input.Keyboard
import de.randomerror.genetictank.input.Keyboard.keyDown
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D

/**
 * Created by henri on 19.10.16.
 */

class Tank(xPos: Double, yPos: Double, val color: Color) : Entity() {

    init {
        this.x = xPos
        this.y = yPos
        velX = 150.0
        velY = 150.0
    }

    var alive = true

    val width = 30.0
    val height = 50.0

    var heading = 0.0
    val velRotation = 4.0

    val actions = mapOf<KeyCode, (Double) -> Unit>(
            KeyCode.W to { deltaTime ->
                x += Math.sin(heading) * velX * deltaTime
                y -= Math.cos(heading) * velY * deltaTime
            },
            KeyCode.S to { deltaTime ->
                x -= Math.sin(heading) * velX * deltaTime
                y += Math.cos(heading) * velY * deltaTime
            },
            KeyCode.A to { deltaTime ->
                heading -= deltaTime * velRotation
            },
            KeyCode.D to { deltaTime ->
                heading += deltaTime * velRotation
            })

    override fun render(gc: GraphicsContext) = gc.transformContext {
        if (!alive) return@transformContext

        val bounds = getBounds().bounds
        gc.strokeRect(bounds.minX.toDouble(), bounds.minY.toDouble(), bounds.width.toDouble(), bounds.height.toDouble())

        translate(x, y)

        rotate(Math.toDegrees(heading), width / 2, height / 2)

        stroke = Color(0.0, 0.0, 0.0, 1.0)
        lineWidth = 1.0

        fill = color
        fillRect(0.0, 0.0, width, height)
        strokeRect(0.0, 0.0, width, height)

        fill = color.brighter()
        fillRect(0.4 * width, -0.2 * width, 0.2 * width, 0.8 * width)
        strokeRect(0.4 * width, -0.2 * width, 0.2 * width, 0.8 * width)

        fillOval(0.1 * width, (height - 0.8 * width) / 2, 0.8 * width, 0.8 * width)
        strokeOval(0.1 * width, (height - 0.8 * width) / 2, 0.8 * width, 0.8 * width)
    }

    override fun update(deltaTime: Double) {
        if (!alive) return

        actions.filter { keyDown(it.key) }.forEach { key, action ->
            action(deltaTime)
        }

        if (Keyboard.keyDown(KeyCode.M, once = true)) {
            val px = x + width / 2 + Math.sin(heading) * (height * 2 / 3)
            val py = y + height / 2 - Math.cos(heading) * (height * 2 / 3)
            GameLoop.entities += Projectile(px, py, heading)
        }

        if (Keyboard.keyDown(KeyCode.C))
            GameLoop.entities.removeAll { it is Projectile }
    }

    override fun collides(x: Double, y: Double): Boolean {
        val bounds = getBounds()

        return bounds.contains(x, y)
    }

    private fun getBounds(): Area {
        val area = Area(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), 0f, 0f))
        return area.createTransformedArea(AffineTransform.getRotateInstance(heading, x + width / 2, y + height / 2))
    }
}
