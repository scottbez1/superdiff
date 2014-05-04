package com.scottbezek.superdiff.manager;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

/**
 * A stream of states. Caches the most recent state, and provides that when new {@link
 * com.scottbezek.superdiff.manager.StateStream.Listener}s subscribe for changes.
 */
public class StateStream<StateType> {

    private final Object mStateLock = new Object();

    @GuardedBy("mStateLock")
    private StateType mCurrentState;

    @GuardedBy("mStateLock")
    private final Map<Listener<StateType>, Handler>
            mListeners = new HashMap<Listener<StateType>, Handler>();

    public StateStream(StateType initialState) {
        mCurrentState = initialState;
    }

    private Runnable getRunnableForListenerCallback(final Listener<StateType> listener,
            final Handler handler, final StateType state) {
        return new Runnable() {
            @Override
            public void run() {
                // Ensure the listener hasn't been unregistered or changed Loopers in the meantime
                synchronized (mStateLock) {
                    if (!handler.equals(mListeners.get(listener))) {
                        return;
                    }
                }
                listener.onStateChanged(state);
            }
        };
    }

    public void update(@Nonnull final StateType newState) {
        synchronized (mStateLock) {
            mCurrentState = newState;

            // Copy the entries to avoid ConcurrentModificationExceptions while notifying
            Set<Pair<Listener<StateType>, Handler>> entrySetCopy =
                    new HashSet<Pair<Listener<StateType>, Handler>>();
            for (Entry<Listener<StateType>, Handler> entry : mListeners.entrySet()) {
                entrySetCopy.add(new Pair<Listener<StateType>, Handler>(entry.getKey(),
                        entry.getValue()));
            }

            for (Pair<Listener<StateType>, Handler> entry : entrySetCopy) {
                entry.second
                        .post(getRunnableForListenerCallback(entry.first, entry.second, newState));
            }
        }
    }

    /**
     * Subscribe for change callbacks (which will be delivered using the same {@link
     * android.os.Looper} as this method is called on). Before this method returns, the provided
     * listener's callback will be invoked with the current state.
     */
    public void subscribeInvoke(Listener<StateType> listener) {
        Looper currentLooper = Looper.myLooper();
        if (currentLooper == null) {
            throw new IllegalStateException("Must be called on a Looper thread");
        }
        synchronized (mStateLock) {
            if (mListeners.containsKey(listener)) {
                throw new IllegalStateException("Listener already registered");
            }
            mListeners.put(listener, new Handler(currentLooper));
            listener.onStateChanged(mCurrentState);
        }
    }

    /**
     * Unsubscribe a listener. Must be called on the same Looper thread under which it was
     * subscribed.
     */
    public void unsubscribe(Listener<StateType> listener) {
        Looper currentLooper = Looper.myLooper();
        if (currentLooper == null) {
            throw new IllegalStateException("Must be called on a Looper thread");
        }
        synchronized (mStateLock) {
            Handler oldRegisteredHandler = mListeners.get(listener);
            if (oldRegisteredHandler == null) {
                throw new IllegalStateException("Listener not registered");
            } else if (!oldRegisteredHandler.getLooper().equals(currentLooper)) {
                throw new IllegalStateException(
                        "Listener is currently registered under a different Looper");
            }
        }
    }

    public interface Listener<StateType> {

        /**
         * Called when the state changes. Invoked on the {@link android.os.Looper} for which this
         * {@link com.scottbezek.superdiff.manager.StateStream.Listener} was registered.
         *
         * @param state The new state value.
         */
        void onStateChanged(StateType state);
    }
}
