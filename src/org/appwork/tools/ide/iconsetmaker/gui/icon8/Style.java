package org.appwork.tools.ide.iconsetmaker.gui.icon8;

public enum Style {
    ALL("All", null),
    IOS9("iOS 9", "ios7"),
    WIN10("Windows 10", "win10"),
    WIN8("Windows 8", "win8"),
    AND5("Android 5 Lollipop", "androidL"),
    AND4("Android 4", "android"),
    COLOR("Color", "color");

    private String key;

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    private String label;

    private Style(String label, String key) {
        this.key = key;
        this.label = label;

    }
}