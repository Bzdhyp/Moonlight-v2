package wtf.moonlight.module;

public enum Subcategory {
    OVERLAY(Category.RENDER, "Overlay"),
    WORLD(Category.RENDER, "World");

    private final Category category;
    private final String name;

    Subcategory(Category category, String name) {
        this.category = category;
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }
}
