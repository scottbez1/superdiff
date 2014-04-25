package com.scottbezek.superdiff.diffcompute;

import java.util.ArrayList;
import java.util.List;

public class DiffCatalog {

    private final List<Region> mLeftUniqueRegions = new ArrayList<Region>();
    private final List<Region> mRightUniqueRegions = new ArrayList<Region>();

    public void addLeftUniqueRegion(Region region) {
        mLeftUniqueRegions.add(region);
    }

    public void addRightUniqueRegion(Region region) {
        mRightUniqueRegions.add(region);
    }

    public List<Region> getLeftUniqueRegions() {
        return mLeftUniqueRegions;
    }

    public List<Region> getRightUniqueRegions() {
        return mRightUniqueRegions;
    }
}
