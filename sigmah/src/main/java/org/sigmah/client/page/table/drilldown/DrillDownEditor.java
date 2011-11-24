/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.client.page.table.drilldown;

import org.sigmah.client.AppEvents;
import org.sigmah.client.EventBus;
import org.sigmah.client.dispatch.Dispatcher;
import org.sigmah.client.event.PivotCellEvent;
import org.sigmah.client.page.common.Shutdownable;
import org.sigmah.client.page.entry.SiteGridPanel;
import org.sigmah.client.page.entry.grouping.NullGroupingModel;
import org.sigmah.client.util.state.StateProvider;
import org.sigmah.shared.command.Filter;
import org.sigmah.shared.report.content.EntityCategory;
import org.sigmah.shared.report.content.PivotTableData;
import org.sigmah.shared.report.model.DimensionType;
import org.sigmah.shared.util.date.DateUtil;

import com.extjs.gxt.ui.client.event.Listener;

public class DrillDownEditor implements Shutdownable {
  
    private final EventBus eventBus;
    private final Dispatcher service;
    private final DateUtil dateUtil;
    private Listener<PivotCellEvent> eventListener;
    private SiteGridPanel gridPanel;

    public DrillDownEditor(EventBus eventBus, Dispatcher service, StateProvider stateMgr, DateUtil dateUtil) {

        this.eventBus = eventBus;
        this.dateUtil = dateUtil;
        this.service = service;
        this.gridPanel = new SiteGridPanel(service);

        eventListener = new Listener<PivotCellEvent>() {
            @Override
			public void handleEvent(PivotCellEvent be) {
                onDrillDown(be);
            }
        };
        eventBus.addListener(AppEvents.Drilldown, eventListener);
    }

    @Override
    public void shutdown() {
        eventBus.removeListener(AppEvents.Drilldown, eventListener);
    } 

    public void onDrillDown(PivotCellEvent event) {

        // construct our filter from the intersection of rows and columns
        Filter filter = new Filter(filterFromAxis(event.getRow()), filterFromAxis(event.getColumn()));

        // apply the effective filter
        final Filter effectiveFilter = new Filter(filter, event.getElement().getContent().getEffectiveFilter());

        // determine the indicator
        final int indicatorId = effectiveFilter.getRestrictions(DimensionType.Indicator).iterator().next();
        effectiveFilter.clearRestrictions(DimensionType.Indicator);

        gridPanel.load(NullGroupingModel.INSTANCE, effectiveFilter);
    }

    private Filter filterFromAxis(PivotTableData.Axis axis) {

        Filter filter = new Filter();
        while (axis != null) {
            if (axis.getDimension() != null) {
                if (axis.getDimension().getType() == DimensionType.Date) {
                    filter.setDateRange(dateUtil.rangeFromCategory(axis.getCategory()));
                } else if (axis.getCategory() instanceof EntityCategory) {
                    filter.addRestriction(axis.getDimension().getType(), ((EntityCategory) axis.getCategory()).getId());
                }
            }
            axis = axis.getParent();
        }
        return filter;
    }

	public SiteGridPanel getGridPanel() {
		return gridPanel;
	}
}


