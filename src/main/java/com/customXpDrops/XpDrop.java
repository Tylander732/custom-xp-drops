package com.customXpDrops;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.Actor;
import net.runelite.api.Skill;

@Data
@AllArgsConstructor
public class XpDrop {
	Skill skill;
	int experience;
	XpDropStyle style;
	boolean fake;
	Actor attachedActor;
}
