package backend.models;

import java.util.Collections;
import java.util.List;

/**
 * A node in the category/operation menu tree that the UI renders dynamically.
 *
 * <p>A {@link Type#DROPDOWN} node groups {@link #getChildren() children}
 * (e.g. "Calculus" → "Derivative", "Integral"). A {@link Type#ACTION} node is
 * a leaf that triggers a specific operation identified by its
 * {@link #getActionKey() action key}, which the {@code MathEngine} uses for
 * routing.
 */
public class ButtonNode {

    public enum Type { DROPDOWN, ACTION }

    private final String label;
    private final Type type;
    private final String actionKey;
    private final List<ButtonNode> children;

    private ButtonNode(String label, Type type, String actionKey, List<ButtonNode> children) {
        this.label = label;
        this.type = type;
        this.actionKey = actionKey;
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    /** Creates a grouping node that expands to reveal its children. */
    public static ButtonNode dropdown(String label, ButtonNode... children) {
        return new ButtonNode(label, Type.DROPDOWN, null, List.of(children));
    }

    /** Creates a leaf node that triggers the operation identified by {@code actionKey}. */
    public static ButtonNode action(String label, String actionKey) {
        return new ButtonNode(label, Type.ACTION, actionKey, null);
    }

    public String getLabel() {
        return label;
    }

    public Type getType() {
        return type;
    }

    /** Non-null for {@link Type#ACTION} nodes; {@code null} for dropdowns. */
    public String getActionKey() {
        return actionKey;
    }

    public List<ButtonNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isAction() {
        return type == Type.ACTION;
    }

    public boolean isDropdown() {
        return type == Type.DROPDOWN;
    }
}
