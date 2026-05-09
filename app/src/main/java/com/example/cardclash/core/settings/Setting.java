package com.example.cardclash.core.settings;

import java.util.Collections;
import java.util.List;

/** A single host-configurable rule. The PreGameSettings UI renders one of these
 *  per row, by type. */
public class Setting {
    public final String key;
    public final String label;
    public final String help;
    public final SettingType type;
    public final Object defaultValue;
    public final int min, max, step;
    public final List<String> options;     // for ENUM_PICKER / LIST_EDITOR options
    public final List<Integer> intOptions; // for INT_PICKER

    private Setting(Builder b) {
        this.key = b.key;
        this.label = b.label;
        this.help = b.help;
        this.type = b.type;
        this.defaultValue = b.defaultValue;
        this.min = b.min;
        this.max = b.max;
        this.step = b.step;
        this.options = b.options == null ? Collections.emptyList() : b.options;
        this.intOptions = b.intOptions == null ? Collections.emptyList() : b.intOptions;
    }

    public static Builder slider(String key, String label, int def, int min, int max, int step) {
        return new Builder(key, label, SettingType.INT_SLIDER).def(def).range(min, max, step);
    }

    public static Builder picker(String key, String label, int def, List<Integer> options) {
        return new Builder(key, label, SettingType.INT_PICKER).def(def).intOptions(options);
    }

    public static Builder toggle(String key, String label, boolean def) {
        return new Builder(key, label, SettingType.TOGGLE).def(def);
    }

    public static Builder enumPicker(String key, String label, String def, List<String> options) {
        return new Builder(key, label, SettingType.ENUM_PICKER).def(def).options(options);
    }

    public static Builder listEditor(String key, String label, List<String> def, List<String> all) {
        return new Builder(key, label, SettingType.LIST_EDITOR).def(def).options(all);
    }

    public static class Builder {
        private final String key, label;
        private final SettingType type;
        private String help;
        private Object defaultValue;
        private int min, max, step = 1;
        private List<String> options;
        private List<Integer> intOptions;

        public Builder(String key, String label, SettingType type) {
            this.key = key; this.label = label; this.type = type;
        }
        public Builder help(String h) { this.help = h; return this; }
        public Builder def(Object d) { this.defaultValue = d; return this; }
        public Builder range(int min, int max, int step) {
            this.min = min; this.max = max; this.step = step; return this;
        }
        public Builder options(List<String> o) { this.options = o; return this; }
        public Builder intOptions(List<Integer> o) { this.intOptions = o; return this; }
        public Setting build() { return new Setting(this); }
    }
}
