/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.shared.report.content;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PieMapMarker extends BubbleMapMarker {

    public static class SliceValue implements Serializable {
        private double value;
        private DimensionCategory category;
        private int color;

        public SliceValue() {
        }

        public SliceValue(SliceValue slice) {
            this.value = slice.value;
            this.category = slice.category;
            this.color = slice.color;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public DimensionCategory getCategory() {
            return category;
        }

        public void setCategory(DimensionCategory category) {
            this.category = category;
        }

        public int getColor() {
            return color;
        }

        public void setColor(int color) {
            this.color = color;
        }
    }

    public PieMapMarker() {
    }

    private List<SliceValue> slices = new ArrayList<SliceValue>(0);

    public List<SliceValue> getSlices() {
        return slices;
    }

    public void setSlices(List<SliceValue> slices) {
        this.slices = slices;
    }
}
