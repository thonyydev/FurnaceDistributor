package com.thonyy.furnacedistributor.client;

/** Priority of C: finish a pending selection, start a contextual one, or reuse the saved area. */
public final class CollectionShortcut {
    public enum Action { SELECT_FIRST, SELECT_SECOND, COLLECT_SAVED }

    public static Action resolve(boolean selecting, boolean savedArea, boolean sneaking, boolean furnaceTarget) {
        if (selecting) return Action.SELECT_SECOND;
        if (sneaking && furnaceTarget) return Action.SELECT_FIRST;
        return savedArea ? Action.COLLECT_SAVED : Action.SELECT_FIRST;
    }

    private CollectionShortcut() { }
}
