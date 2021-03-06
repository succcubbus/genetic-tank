package de.randomerror.genetictank.entities

import de.randomerror.genetictank.GameLoop
import de.randomerror.genetictank.genetic.Player
import de.randomerror.genetictank.helper.RotatedRectangle
import de.randomerror.genetictank.helper.getBounds
import de.randomerror.genetictank.helper.rotate
import de.randomerror.genetictank.helper.transformContext
import de.randomerror.genetictank.labyrinth.Point
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color

/**
 * Created by henri on 19.10.16.
 */
class Tank(xPos: Double, yPos: Double, val player: Player) : Entity() {

    init {
        this.x = xPos
        this.y = yPos
    }

    var entities = GameLoop.entities
    var labyrinth = GameLoop.labyrinth
    var collidesX = false
    var collidesY = false

    var game = GameLoop

    var heading = 0.0

    val velocity = 150.0
    val velRotation = 4.0

    val width = 30.0
    val height = 50.0

    var alive = true

    var movedDistance = 0.0
    var shotBullets = 0
    var blockedTicks = 0
    val visitedTiles = mutableSetOf<Point>()

    val bullets = mutableListOf<Projectile>()

    var color: Color? = null

    override fun render(gc: GraphicsContext) = gc.transformContext {
        if (!alive) return@transformContext

        if (color == null)
            color = Color.color(Math.random(), Math.random(), Math.random())

        val outline = getBounds().outline
        gc.strokeRect(outline.x, outline.y, outline.width, outline.height)

        translate(x, y)
        rotate(Math.toDegrees(heading), width / 2, height / 2)

        stroke = Color(0.0, 0.0, 0.0, 1.0)
        lineWidth = 1.0

        fill = color
        fillRect(0.0, 0.0, width, height)
        strokeRect(0.0, 0.0, width, height)

        fill = color?.brighter()
        fillRect(0.4 * width, -0.2 * width, 0.2 * width, 0.8 * width)
        strokeRect(0.4 * width, -0.2 * width, 0.2 * width, 0.8 * width)

        fillOval(0.1 * width, (height - 0.8 * width) / 2, 0.8 * width, 0.8 * width)
        strokeOval(0.1 * width, (height - 0.8 * width) / 2, 0.8 * width, 0.8 * width)
    }

    override fun update(deltaTime: Double) {
        if (!alive) return

        player.update(deltaTime, this)

        var (velX, velY, velH) = getAttemptedMove()
        val (testX, testY, testH) = getNewPosition(deltaTime)

        collidesX = false
        collidesY = false
        val walls = entities
                .filter { it is Wall }
                .map { it as Wall }
                .filter { Math.abs(x - it.x) < 150 && Math.abs(y - it.y) < 150 }

        if (!walls.isEmpty()) {
            val testBoundsX = RotatedRectangle(testX, y, width, height, heading)
            walls.filter { testBoundsX.collidesWith(it.bounds.toRotatedRectangle()) }.forEach { wall ->
                velX = 0.0
                velY = 0.0
                collidesX = true
            }
            val testBoundsY = RotatedRectangle(x, testY, width, height, heading)
            walls.filter { testBoundsY.collidesWith(it.bounds.toRotatedRectangle()) }.forEach { wall ->
                velX = 0.0
                velY = 0.0
                collidesY = true
            }

            val testBoundsH = RotatedRectangle(x, y, width, height, testH)
            walls.filter { testBoundsH.collidesWith(it.bounds.toRotatedRectangle()) }.forEach { wall ->
                val collision = testBoundsH.collisionArea(wall.bounds)

                if (collision.area.isEmpty) return@forEach

                val midX = collision.x + collision.width / 2
                val midY = collision.y + collision.height / 2

                if (Math.min(Math.abs(midX - wall.x), Math.abs(midX - (wall.x + wall.width))) > Math.min(Math.abs(midY - wall.y), Math.abs(midY - (wall.y + wall.height)))) {
                    if (y + height / 2 < wall.y + wall.height / 2) {
                        y -= Math.abs(wall.y - (collision.y + collision.height))
                    } else {
                        y += Math.abs(wall.y + wall.height - (collision.y))
                    }
                } else {
                    if (x + width / 2 < wall.x + wall.width / 2) {
                        x -= Math.abs(wall.x - (collision.x + collision.width))
                    } else {
                        x += Math.abs(wall.x + wall.width - (collision.x))
                    }
                }
            }
        }

        x += velX * deltaTime
        y += velY * deltaTime
        heading += velH * deltaTime

        if (player.forward() && !player.backward()) {
            movedDistance += Math.abs(velX * deltaTime)
            movedDistance += Math.abs(velY * deltaTime)
        }

        if ((player.forward() xor player.backward()) && (collidesX || collidesY)) {
            blockedTicks++
        }

        val (tileW, tileH) = labyrinth.getTileSize()
        val tileX = ((x + width / 2) / tileW).toInt() * 2 + 1
        val tileY = ((y + height / 2) / tileH).toInt() * 2 + 1
        visitedTiles += Point(tileX, tileY)

        bullets.removeAll { !it.alive }

        if (player.shoot() && bullets.size < 5) {
            val px = x + width / 2 + Math.sin(heading) * (height / 2)
            val py = y + height / 2 - Math.cos(heading) * (height / 2)
            val projectile = Projectile(px, py, heading)
            projectile.entities = entities
            entities.add(projectile)
            bullets += projectile
            shotBullets++
        }
    }

    private fun getNewPosition(deltaTime: Double): Triple<Double, Double, Double> {
        val (velX, velY, velH) = getAttemptedMove()

        val testX = x + velX * deltaTime
        val testY = y + velY * deltaTime
        val testH = heading + velH * deltaTime

        return Triple(testX, testY, testH)
    }

    private fun getAttemptedMove(): Triple<Double, Double, Double> {
        val (attemptedVelX, attemptedVelY) = if (player.forward() && !player.backward()) {
            velocity * Math.sin(heading) to -velocity * Math.cos(heading)
        } else if (player.backward() && !player.forward()) {
            -velocity * Math.sin(heading) to velocity * Math.cos(heading)
        } else 0.0 to 0.0

        val attemptedRotation = if (player.turnLeft() && !player.turnRight()) {
            -velRotation
        } else if (player.turnRight() && !player.turnLeft()) {
            velRotation
        } else 0.0

        return Triple(attemptedVelX, attemptedVelY, attemptedRotation)
    }

    override fun collides(x: Double, y: Double) = getBounds().collidesWith(x, y)
}
