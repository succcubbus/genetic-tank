package de.randomerror.genetictank.helper

import de.randomerror.genetictank.entities.Entity
import javafx.scene.canvas.GraphicsContext

/**
 * Run the provided code block on the GraphicsContext and reset the transform afterwards
 */
inline fun GraphicsContext.transformContext(block: GraphicsContext.() -> Unit) {
    save()
    block()
    restore()
}

/**
 * Rotate the GraphicsContext around x, y coordinates
 */
fun GraphicsContext.rotate(degrees: Double, x: Double, y: Double) {
    translate(x, y)
    rotate(degrees)
    translate(-x, -y)
}

fun GraphicsContext.render(entity: Entity) = entity.render(this)