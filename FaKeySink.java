package wtf.carnation.ui.fatality;

/** Receives keyboard input while it holds the interface's focus. */
interface FaKeySink {
    boolean keyPressed(int key, int modifiers);

    default boolean charTyped(char character) {
        return false;
    }

    /** Focus moved elsewhere (click outside, screen closed). */
    default void focusLost() {
    }
}
