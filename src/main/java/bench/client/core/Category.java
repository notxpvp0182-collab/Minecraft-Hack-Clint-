package bench.client.core;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    WORLD("World"),
    MISC("Misc"),
    EXPLOIT("Exploit"),
    FUN("Fun");

    private final String title;
    Category(String title) { this.title = title; }
    public String getTitle() { return title; }
}
