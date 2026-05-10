package com.example.cardclash.ui.common;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.cardclash.core.theme.Theme;
import com.example.cardclash.core.theme.ThemePrefs;

/**
 * A LinearLayout that hosts a single content child and a chevron handle. Tapping
 * the handle toggles the content's visibility, animating layout changes via a
 * {@link LayoutTransition}. The chevron rotates 180° to indicate state.
 *
 * <p>Use {@link #setEdge} to pick which side of the screen the panel attaches to —
 * that determines whether the handle sits on the leading, trailing, top, or bottom
 * side of the content.
 *
 * <p>Children declared in XML or programmatically are treated as the panel's
 * content. The handle is owned by the panel itself.
 */
public class CollapsiblePanel extends LinearLayout {

    public enum Edge { LEFT, RIGHT, TOP, BOTTOM }

    private Edge edge = Edge.LEFT;
    private boolean expanded = true;
    private TextView handleChevron;
    private TextView handleLabel;
    private LinearLayout handleContainer;
    private FrameLayout contentContainer;
    private boolean built;

    public CollapsiblePanel(Context ctx) { this(ctx, null); }
    public CollapsiblePanel(Context ctx, @Nullable AttributeSet attrs) { this(ctx, attrs, 0); }
    public CollapsiblePanel(Context ctx, @Nullable AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
    }

    /** Configure the edge AND the panel's orientation. Call before adding content. */
    public void setEdge(Edge e) {
        this.edge = e;
        if (built) rebuildSkeleton();
    }

    /** Set the optional rotated label shown next to the chevron when collapsed. */
    public void setHandleLabel(CharSequence text) {
        if (!built) buildSkeleton();
        handleLabel.setText(text);
        handleLabel.setVisibility(text == null || text.length() == 0 ? GONE : VISIBLE);
    }

    public void setExpanded(boolean expand, boolean animate) {
        if (this.expanded == expand) return;
        this.expanded = expand;
        if (!built) return;
        if (!animate) {
            LayoutTransition lt = getLayoutTransition();
            setLayoutTransition(null);
            contentContainer.setVisibility(expand ? VISIBLE : GONE);
            setLayoutTransition(lt);
        } else {
            contentContainer.setVisibility(expand ? VISIBLE : GONE);
        }
        handleChevron.animate().rotation(rotationFor(expand)).setDuration(180).start();
    }

    public boolean isExpanded() { return expanded; }
    public void toggle() { setExpanded(!expanded, true); }

    @Override public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!built) {
            buildSkeleton();
            super.addView(child, index, params);
            return;
        }
        if (child == handleContainer || child == contentContainer) {
            super.addView(child, index, params);
        } else {
            // funnel external adds into the content container
            contentContainer.addView(child, params);
        }
    }

    private void buildSkeleton() {
        built = true;
        rebuildSkeleton();
    }

    private void rebuildSkeleton() {
        // Move existing direct children into contentContainer
        Theme t = ThemePrefs.activeTheme(getContext());
        boolean horizontal = (edge == Edge.LEFT || edge == Edge.RIGHT);
        setOrientation(horizontal ? HORIZONTAL : VERTICAL);
        setLayoutTransition(new LayoutTransition());

        // Drain pre-existing children (if any) — this happens when caller built UI before calling setEdge
        FrameLayout newContent = new FrameLayout(getContext());
        if (contentContainer != null) {
            // move children
            int n = contentContainer.getChildCount();
            for (int i = 0; i < n; i++) {
                View child = contentContainer.getChildAt(0);
                contentContainer.removeViewAt(0);
                newContent.addView(child);
            }
        } else {
            // first build — drain any direct children that were added before skeleton existed
            int n = getChildCount();
            for (int i = 0; i < n; i++) {
                View child = getChildAt(0);
                super.removeViewAt(0);
                newContent.addView(child);
            }
        }
        contentContainer = newContent;
        super.removeAllViews();

        handleContainer = new LinearLayout(getContext());
        handleContainer.setOrientation(horizontal ? VERTICAL : HORIZONTAL);
        handleContainer.setGravity(Gravity.CENTER);
        int hp = dp(8);
        handleContainer.setPadding(hp, hp, hp, hp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(t.colorSurface());
        bg.setStroke(dp(t.borderWidthDp()), t.colorBorder());
        bg.setCornerRadius(dp(6));
        handleContainer.setBackground(bg);
        handleContainer.setClickable(true);
        handleContainer.setFocusable(true);
        handleContainer.setOnClickListener(v -> toggle());

        handleChevron = new TextView(getContext());
        handleChevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        handleChevron.setText("▾");
        handleChevron.setTextColor(t.colorFg1());
        handleChevron.setTypeface(handleChevron.getTypeface(), Typeface.BOLD);
        handleChevron.setRotation(rotationFor(expanded));
        handleContainer.addView(handleChevron);

        handleLabel = new TextView(getContext());
        handleLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        handleLabel.setLetterSpacing(0.16f);
        handleLabel.setTextColor(t.colorFg2());
        handleLabel.setTypeface(handleLabel.getTypeface(), Typeface.BOLD);
        handleLabel.setVisibility(GONE);
        if (horizontal) handleLabel.setRotation(edge == Edge.LEFT ? 90f : -90f);
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (horizontal) lblLp.topMargin = dp(8); else lblLp.leftMargin = dp(8);
        handleContainer.addView(handleLabel, lblLp);

        // assemble in correct order based on edge
        LayoutParams contentLp = horizontal
                ? new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                : new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        LayoutParams handleLp = horizontal
                ? new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
                : new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        switch (edge) {
            case LEFT:   super.addView(contentContainer, contentLp); super.addView(handleContainer, handleLp); break;
            case RIGHT:  super.addView(handleContainer, handleLp);   super.addView(contentContainer, contentLp); break;
            case TOP:    super.addView(contentContainer, contentLp); super.addView(handleContainer, handleLp); break;
            case BOTTOM: super.addView(handleContainer, handleLp);   super.addView(contentContainer, contentLp); break;
        }
        contentContainer.setVisibility(expanded ? VISIBLE : GONE);
    }

    private float rotationFor(boolean expand) {
        // chevron points TOWARD the content when expanded (so collapse direction is intuitive)
        switch (edge) {
            case LEFT:   return expand ? 0f   : 180f; // ▾ vs ▴ rotated
            case RIGHT:  return expand ? 180f : 0f;
            case TOP:    return expand ? 0f   : 180f;
            case BOTTOM: return expand ? 180f : 0f;
        }
        return 0f;
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
