package app.fiber.event.events

import app.fiber.event.Event
import app.fiber.model.Deployment

class DeploymentDeletedEvent(val deployment: Deployment) : Event