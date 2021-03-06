package bloom.com.stickyrecycler;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class StickyGridLayoutManager extends GridLayoutManager {



    private StickyHeaderPositioner positioner;
    private List<MultiAdapterItem> headerHandler;
    private List<Integer> headerPositions = new ArrayList<>();
    private ViewRetriever.RecyclerViewRetriever viewRetriever;
    private int headerElevation = StickyHeaderPositioner.NO_ELEVATION;
    @Nullable
    private StickyHeaderListener listener;
    private int headerType;


    public StickyGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public StickyGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public StickyGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }
    private void init(List<MultiAdapterItem> stickyHeaderHandler) {
        Preconditions.checkNotNull(stickyHeaderHandler, "StickyHeaderHandler == null");
        this.headerHandler = stickyHeaderHandler;
    }

    /**
     * Register a callback to be invoked when a header is attached/re-bound or detached.
     *
     * @param listener The callback that will be invoked, or null to unset.
     */
    public void setStickyHeaderListener(@Nullable StickyHeaderListener listener) {
        this.listener = listener;
        if (positioner != null) {
            positioner.setListener(listener);
        }
    }

    /**
     * Enable or disable elevation for Sticky Headers.
     * <p>
     * If you want to specify a specific amount of elevation, use
     * {@link StickyLinearLayoutManager#elevateHeaders(int)}
     *
     * @param elevateHeaders Enable Sticky Header elevation. Default is false.
     */
    public void elevateHeaders(boolean elevateHeaders) {
        this.headerElevation = elevateHeaders ?
                StickyHeaderPositioner.DEFAULT_ELEVATION : StickyHeaderPositioner.NO_ELEVATION;
        elevateHeaders(headerElevation);
    }

    /**
     * Enable Sticky Header elevation with a specific amount.
     *
     * @param dp elevation in dp
     */
    public void elevateHeaders(int dp) {
        this.headerElevation = dp;
        if (positioner != null) {
            positioner.setElevateHeaders(dp);
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        cacheHeaderPositions();
        if (positioner != null) {
            runPositionerInit();
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scroll = super.scrollVerticallyBy(dy, recycler, state);
        if(this.headerType!=VERTICAL)
            return scroll;
        if (Math.abs(scroll) > 0) {
            if (positioner != null) {
                positioner.updateHeaderState(
                        findFirstVisibleItemPosition(), getVisibleHeaders(), viewRetriever, findFirstCompletelyVisibleItemPosition() == 0);
            }
        }
        return scroll;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int scroll = super.scrollHorizontallyBy(dx, recycler, state);
        if(this.headerType!=HORIZONTAL)
            return scroll;
        if (Math.abs(scroll) > 0) {
            if (positioner != null) {
                positioner.updateHeaderState(
                        findFirstVisibleItemPosition(), getVisibleHeaders(), viewRetriever, findFirstCompletelyVisibleItemPosition() == 0);
            }
        }
        return scroll;
    }

    @Override
    public void removeAndRecycleAllViews(RecyclerView.Recycler recycler) {
        super.removeAndRecycleAllViews(recycler);
        if (positioner != null) {
            positioner.clearHeader();
        }
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        Preconditions.validateParentView(view);
        viewRetriever = new ViewRetriever.RecyclerViewRetriever(view);
        positioner = new StickyHeaderPositioner(view);
        positioner.setElevateHeaders(headerElevation);
        positioner.setListener(listener);
        if (headerPositions.size() > 0) {
            // Layout has already happened and header positions are cached. Catch positioner up.
            positioner.setHeaderPositions(headerPositions);
            runPositionerInit();
        }
        super.onAttachedToWindow(view);
    }

    private void runPositionerInit() {
        positioner.reset(getOrientation());
        positioner.updateHeaderState(findFirstVisibleItemPosition(), getVisibleHeaders(), viewRetriever, findFirstCompletelyVisibleItemPosition() == 0);
    }

    private Map<Integer, View> getVisibleHeaders() {
        Map<Integer, View> visibleHeaders = new LinkedHashMap<>();

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int dataPosition = getPosition(view);
            if (headerPositions.contains(dataPosition)) {
                visibleHeaders.put(dataPosition, view);
            }
        }
        return visibleHeaders;
    }

    private void cacheHeaderPositions() {
        headerPositions.clear();
        List<?> adapterData = (List<?>) headerHandler;
        if (adapterData == null) {
            if (positioner != null) {
                positioner.setHeaderPositions(headerPositions);
            }
            return;
        }

        for (int i = 0; i < adapterData.size(); i++) {
            if (adapterData.get(i) instanceof StickyHeader) {
                headerPositions.add(i);
            }
        }
        if (positioner != null) {
            positioner.setHeaderPositions(headerPositions);
        }
    }

    public void setHeaderType(int headerType) {
        this.headerType = headerType;
    }
}
