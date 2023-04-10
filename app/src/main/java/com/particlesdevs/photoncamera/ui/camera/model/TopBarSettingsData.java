

package com.particlesdevs.photoncamera.ui.camera.model;

public class TopBarSettingsData<T, V extends Comparable<? super V>> {
    private final T type;
    private V value;

    public TopBarSettingsData(T type) {
        this.type = type;
    }

    public TopBarSettingsData(T type, V value) {
        this.type = type;
        this.value = value;
    }

    public T getType() {
        return type;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }
}
