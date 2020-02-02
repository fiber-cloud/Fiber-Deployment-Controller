package app.fiber.event

import app.fiber.model.Deployment

class DeploymentUpdatedEvent(val deployment: Deployment) : Event

class DeploymentDeletedEvent(val deployment: Deployment) : Event