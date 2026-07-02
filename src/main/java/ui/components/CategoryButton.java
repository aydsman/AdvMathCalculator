package ui.components;

import backend.models.ButtonNode;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

import java.util.function.BiConsumer;

/**
 * A dropdown built dynamically from a {@link ButtonNode} tree. Dropdown nodes become
 * nested menus; action nodes become clickable items. When an action is chosen, the
 * button reports the owning category label and the selected action node.
 */
public class CategoryButton extends MenuButton {

    private BiConsumer<String, ButtonNode> onOperationSelected;
    private ButtonNode selected;
    private String selectedCategory;

    public CategoryButton(ButtonNode root) {
        getStyleClass().add("category-button");
        setText("Operation \u25BE");
        for (ButtonNode child : root.getChildren()) {
            getItems().add(buildItem(child, root.getLabel()));
        }
    }

    private MenuItem buildItem(ButtonNode node, String parentCategory) {
        if (node.isDropdown()) {
            Menu menu = new Menu(node.getLabel());
            for (ButtonNode child : node.getChildren()) {
                menu.getItems().add(buildItem(child, node.getLabel()));
            }
            return menu;
        }
        MenuItem item = new MenuItem(node.getLabel());
        item.setOnAction(e -> {
            selected = node;
            selectedCategory = parentCategory;
            setText(parentCategory + " \u00B7 " + node.getLabel());
            if (onOperationSelected != null) {
                onOperationSelected.accept(parentCategory, node);
            }
        });
        return item;
    }

    public void setOnOperationSelected(BiConsumer<String, ButtonNode> handler) {
        this.onOperationSelected = handler;
    }

    public ButtonNode getSelected() {
        return selected;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }
}
