package dev.reactant.mechanism.features.interaction.blocklocation

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.block.Action

class InteractiveLocation(val location: Location, vararg val interactionTypes: Action, val callback: (player: Player) -> Unit)
