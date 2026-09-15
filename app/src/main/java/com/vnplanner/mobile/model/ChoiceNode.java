package com.vnplanner.mobile.model;

import java.util.ArrayList;
import java.util.List;

public class ChoiceNode {
    public String text = "";
    public boolean ending = false;
    public List<ChoiceNode> children = new ArrayList<>();
}
