package com.vnplanner.mobile.model;

import java.util.ArrayList;
import java.util.List;

/** Shared schema matching the desktop Jackson property names. */
public class Project {
    public String title="", workingTitle="", oneSentencePitch="", coreIdea="", genreTone="", playerFeel="";
    public String centralConflict="", beginning="", middle="", revelations="", climax="", ending="", whatChanges="";
    public String protagonistName="", protagonistAge="", protagonistPersonality="", protagonistWant="", protagonistNeed="", protagonistFear="", protagonistArc="";
    public List<CharacterData> characters = new ArrayList<>();
    public List<Chapter> chapters = new ArrayList<>();
    public String amountOfChoice="", importantChoices="", branches="", differentEndings="", endingRequirements="";
    public List<ChoiceNode> choices = new ArrayList<>();
    public String setting="", importantLocations="", worldRules="", loreHistory="", secrets="";
    public String visualStyle="", musicAudio="", uiPresentation="", inspirations="";
    public String engineTools="", mustHave="", niceToHave="", scopeLimits="", freeNotes="";
}
