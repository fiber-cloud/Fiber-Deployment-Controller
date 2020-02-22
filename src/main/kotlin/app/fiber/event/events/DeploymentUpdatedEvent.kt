package app.fiber.event.events

import app.fiber.event.Event
import app.fiber.model.Deployment

class DeploymentUpdatedEvent(val deployment: Deployment) : Event