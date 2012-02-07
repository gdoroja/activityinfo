package org.sigmah.client.page.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sigmah.client.AppEvents;
import org.sigmah.client.EventBus;
import org.sigmah.client.dispatch.AsyncMonitor;
import org.sigmah.client.dispatch.Dispatcher;
import org.sigmah.client.dispatch.monitor.MaskingAsyncMonitor;
import org.sigmah.client.event.PivotCellEvent;
import org.sigmah.client.i18n.I18N;
import org.sigmah.client.icon.IconImageBundle;
import org.sigmah.client.page.common.filter.AdminFilterPanel;
import org.sigmah.client.page.common.filter.DateRangePanel;
import org.sigmah.client.page.common.filter.FilterPanelSet;
import org.sigmah.client.page.common.filter.IndicatorTreePanel;
import org.sigmah.client.page.common.filter.PartnerFilterPanel;
import org.sigmah.client.page.common.toolbar.ActionListener;
import org.sigmah.client.page.common.toolbar.ActionToolBar;
import org.sigmah.client.page.common.toolbar.UIActions;
import org.sigmah.client.page.table.drilldown.DrillDownEditor;
import org.sigmah.client.util.date.DateUtilGWTImpl;
import org.sigmah.client.util.state.StateProvider;
import org.sigmah.shared.command.Filter;
import org.sigmah.shared.command.GeneratePivotTable;
import org.sigmah.shared.command.GetSchema;
import org.sigmah.shared.dto.ActivityDTO;
import org.sigmah.shared.dto.AdminEntityDTO;
import org.sigmah.shared.dto.AdminLevelDTO;
import org.sigmah.shared.dto.AttributeGroupDTO;
import org.sigmah.shared.dto.CountryDTO;
import org.sigmah.shared.dto.DimensionFolder;
import org.sigmah.shared.dto.IndicatorDTO;
import org.sigmah.shared.dto.PartnerDTO;
import org.sigmah.shared.dto.SchemaDTO;
import org.sigmah.shared.dto.UserDatabaseDTO;
import org.sigmah.shared.report.content.PivotContent;
import org.sigmah.shared.report.model.AdminDimension;
import org.sigmah.shared.report.model.AttributeGroupDimension;
import org.sigmah.shared.report.model.DateDimension;
import org.sigmah.shared.report.model.DateUnit;
import org.sigmah.shared.report.model.Dimension;
import org.sigmah.shared.report.model.DimensionType;
import org.sigmah.shared.report.model.PivotTableReportElement;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.BaseTreeLoader;
import com.extjs.gxt.ui.client.data.DataProxy;
import com.extjs.gxt.ui.client.data.DataReader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.data.ModelStringProvider;
import com.extjs.gxt.ui.client.data.TreeLoader;
import com.extjs.gxt.ui.client.dnd.DND;
import com.extjs.gxt.ui.client.dnd.ListViewDragSource;
import com.extjs.gxt.ui.client.dnd.ListViewDropTarget;
import com.extjs.gxt.ui.client.event.CheckChangedEvent;
import com.extjs.gxt.ui.client.event.CheckChangedListener;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.TreePanelEvent;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.ListView;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.layout.AccordionLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayoutData;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AbstractPivot extends LayoutContainer implements ActionListener{

	protected EventBus eventBus;
	protected Dispatcher service;
	protected StateProvider stateMgr;

	protected ListStore<Dimension> rowDims;
	protected ListStore<Dimension> colDims;

	protected TreeStore<ModelData> dimensionStore;
	protected TreePanel<ModelData> treePanel;

	protected ContentPanel filterPane;
	protected IndicatorTreePanel indicatorPanel;
	protected AdminFilterPanel adminPanel;
	protected DateRangePanel datePanel;
	protected PartnerFilterPanel partnerPanel;
	protected LayoutContainer center;
	protected PivotGridPanel gridContainer;
	protected ActionToolBar gridToolBar;
	protected Listener<PivotCellEvent> initialDrillDownListener;
	protected FilterPanelSet filterPanelSet;

	public AbstractPivot(EventBus eventBus, Dispatcher service,
			StateProvider stateMgr) {
		this.eventBus = eventBus;
		this.service = service;
		this.stateMgr = stateMgr;

		this.eventBus = eventBus;
		this.service = service;
		this.stateMgr = stateMgr;

		initializeComponent();

		createPane();
		createFilterPane();
		createIndicatorPanel();
		createAdminFilter();
		createDateFilter();
		createPartnerFilter();
		createGridContainer();

		filterPanelSet = new FilterPanelSet(adminPanel, datePanel, partnerPanel);

		initialDrillDownListener = new Listener<PivotCellEvent>() {
			@Override
			public void handleEvent(PivotCellEvent be) {
				createDrilldownPanel(be);
			}
		};
		eventBus.addListener(AppEvents.DRILL_DOWN, initialDrillDownListener);

	}

	private void initializeComponent() {
		BorderLayout borderLayout = new BorderLayout();
		borderLayout.setEnableState(true);
		setStateId("pivotPage");
		setLayout(borderLayout);
	}

	private void createPane() {

		VBoxLayout layout = new VBoxLayout();
		layout.setPadding(new Padding(5));
		layout.setVBoxLayoutAlign(VBoxLayout.VBoxLayoutAlign.STRETCH);

		ContentPanel pane = new ContentPanel();
		pane.setHeading(I18N.CONSTANTS.dimensions());
		pane.setScrollMode(Style.Scroll.NONE);
		pane.setIcon(null);
		pane.setLayout(layout);

		VBoxLayoutData labelLayout = new VBoxLayoutData();
		VBoxLayoutData listLayout = new VBoxLayoutData();
		listLayout.setFlex(1.0);

		createDimensionsTree();
		pane.add(treePanel, listLayout);
		pane.add(new Text(I18N.CONSTANTS.rows()), labelLayout);

		rowDims = createStore();
		rowDims.add(new Dimension(I18N.CONSTANTS.database(),
				DimensionType.Database));
		rowDims.add(new Dimension(I18N.CONSTANTS.activity(),
				DimensionType.Activity));
		rowDims.add(new Dimension(I18N.CONSTANTS.indicators(),
				DimensionType.Indicator));
		pane.add(createList(rowDims), listLayout);
		pane.add(new Text(I18N.CONSTANTS.columns()), labelLayout);

		colDims = createStore();
		colDims.add(new Dimension(I18N.CONSTANTS.partner(),
				DimensionType.Partner));
		pane.add(createList(colDims), listLayout);

		BorderLayoutData east = new BorderLayoutData(Style.LayoutRegion.EAST);
		east.setCollapsible(true);
		east.setSplit(true);
		east.setMargins(new Margins(0, 5, 0, 0));

		add(pane, east);

	}

	private void createDimensionsTree() {
		TreeLoader<ModelData> loader = new BaseTreeLoader<ModelData>(
				new Proxy()) {
			@Override
			public boolean hasChildren(ModelData parent) {
				if (parent instanceof AttributeGroupDTO) {
					return !((AttributeGroupDTO) parent).isEmpty();
				}
				return parent instanceof DimensionFolder
						|| parent instanceof AttributeGroupDTO;
			}
		};

		// tree store
		dimensionStore = new TreeStore<ModelData>(loader);
		dimensionStore.setKeyProvider(new ModelKeyProvider<ModelData>() {
			@Override
			public String getKey(ModelData model) {
				return "node_" + model.get("id");
			}
		});

		treePanel = new TreePanel<ModelData>(dimensionStore);
		treePanel.setBorders(true);
		treePanel.setCheckable(true);
		treePanel.setCheckNodes(TreePanel.CheckNodes.LEAF);
		treePanel.setCheckStyle(TreePanel.CheckCascade.NONE);
		treePanel.getStyle().setNodeCloseIcon(null);
		treePanel.getStyle().setNodeOpenIcon(null);

		treePanel.setStateful(true);
		treePanel.setLabelProvider(new ModelStringProvider<ModelData>() {
			@Override
			public String getStringValue(ModelData model, String property) {
				return trim((String) model.get("caption"));
			}
		});

		/* enable drag and drop for dev */
		// TreePanelDragSource source = new TreePanelDragSource(treePanel);
		// source.setTreeSource(DND.TreeSource.LEAF);
		/* end enable drag and drop for dev */

		treePanel.setId("statefullavaildims");
		treePanel.collapseAll();

		final ArrayList<ModelData> dimensions = new ArrayList<ModelData>();
		dimensions.add(new Dimension(I18N.CONSTANTS.database(),
				DimensionType.Database));
		dimensions.add(new Dimension(I18N.CONSTANTS.activity(),
				DimensionType.Activity));
		dimensions.add(new Dimension(I18N.CONSTANTS.indicators(),
				DimensionType.Indicator));
		dimensions.add(new Dimension(I18N.CONSTANTS.partner(),
				DimensionType.Partner));
		dimensions.add(new Dimension(I18N.CONSTANTS.project(),
				DimensionType.Project));
		dimensions.add(new Dimension(I18N.CONSTANTS.location(),
				DimensionType.Location));

		dimensions.add(new DimensionFolder(I18N.CONSTANTS.geography(),
				DimensionType.AdminLevel, 0, 0));
		dimensions.add(new DimensionFolder(I18N.CONSTANTS.time(),
				DimensionType.Date, 0, 0));
		dimensions.add(new DimensionFolder(I18N.CONSTANTS.attributes(),
				DimensionType.AttributeGroup, 0, 0));

		dimensionStore.add(dimensions, false);

		setDimensionChecked(dimensions.get(0), true);
		setDimensionChecked(dimensions.get(1), true);
		setDimensionChecked(dimensions.get(2), true);
		setDimensionChecked(dimensions.get(3), true);

		treePanel.addCheckListener(new CheckChangedListener<ModelData>() {
			@Override
			public void checkChanged(CheckChangedEvent<ModelData> event) {
				onDimensionChecked(event);
			}
		});
	}

	private ListStore<Dimension> createStore() {
		ListStore<Dimension> store = new ListStore<Dimension>();
		return store;
	}

	public void addStoreListenerToRowDims(StoreListener listener) {
		rowDims.addStoreListener(listener);
	}

	private ListView createList(ListStore<Dimension> store) {
		ListView<Dimension> list = new ListView<Dimension>(store);
		list.setDisplayProperty("caption");
		ListViewDragSource source = new ListViewDragSource(list);
		ListViewDropTarget target = new ListViewDropTarget(list);
		target.setFeedback(DND.Feedback.INSERT);
		target.setAllowSelfAsSource(true);
		return list;
	}

	private void createFilterPane() {
		filterPane = new ContentPanel();
		filterPane.setHeading(I18N.CONSTANTS.filter());
		filterPane.setIcon(IconImageBundle.ICONS.filter());
		filterPane.setLayout(new AccordionLayout());

		BorderLayoutData west = new BorderLayoutData(Style.LayoutRegion.WEST);
		west.setMinSize(250);
		west.setSize(250);
		west.setCollapsible(true);
		west.setSplit(true);
		west.setMargins(new Margins(0, 0, 0, 0));
		add(filterPane, west);
	}

	private void createIndicatorPanel() {
		indicatorPanel = new IndicatorTreePanel(service, true, getMonitor());
		indicatorPanel.setHeaderVisible(true);
		indicatorPanel.setHeading(I18N.CONSTANTS.indicators());
		indicatorPanel.setIcon(IconImageBundle.ICONS.indicator());
		filterPane.add(indicatorPanel);
	}

	
	private void createAdminFilter() {
		adminPanel = new AdminFilterPanel(service);
		adminPanel.setHeading(I18N.CONSTANTS.filterByGeography());
		adminPanel.setIcon(IconImageBundle.ICONS.filter());
		adminPanel.applyBaseFilter(new Filter());
		filterPane.add(adminPanel);
	}

	private void createDateFilter() {
		datePanel = new DateRangePanel();
		filterPane.add(datePanel);
	}

	private void createPartnerFilter() {
		partnerPanel = new PartnerFilterPanel(service);
		partnerPanel.applyBaseFilter(new Filter());
		filterPane.add(partnerPanel);
	}

	private void onDimensionChecked(CheckChangedEvent<ModelData> event) {
		List<ModelData> checked = event.getCheckedSelection();
		for (ModelData r : rowDims.getModels()) {
			if (checked.contains(r)) {
				checked.remove(r);
			} else {
				rowDims.remove((Dimension) r);
			}
		}

		for (ModelData c : colDims.getModels()) {
			if (checked.contains(c)) {
				checked.remove(c);
			} else {
				colDims.remove((Dimension) c);
			}
		}

		for (ModelData newItem : checked) {
			if (rowDims.getModels().size() > colDims.getModels().size()) {
				colDims.add((Dimension) newItem);
			} else {
				rowDims.add((Dimension) newItem);
			}
		}
	}

	private void createGridContainer() {
		center = new LayoutContainer();
		center.setLayout(new BorderLayout());
		add(center, new BorderLayoutData(Style.LayoutRegion.CENTER));

		gridContainer = new PivotGridPanel(eventBus);
		gridContainer.setHeaderVisible(true);
		gridContainer.setHeading(I18N.CONSTANTS.preview());

		gridToolBar =  new ActionToolBar(this);
		gridContainer.setTopComponent(gridToolBar);

		gridToolBar.addRefreshButton();
		
		center.add(gridContainer, new BorderLayoutData(
				Style.LayoutRegion.CENTER));
	}

	private void createDrilldownPanel(PivotCellEvent event) {
		BorderLayoutData layout = new BorderLayoutData(Style.LayoutRegion.SOUTH);
		layout.setSplit(true);
		layout.setCollapsible(true);

		DrillDownEditor drilldownEditor = new DrillDownEditor(eventBus,
				service, stateMgr, new DateUtilGWTImpl());
		drilldownEditor.onDrillDown(event);

		center.add(drilldownEditor.getGridPanel(), layout);

		// disconnect our initial drilldown listener;
		// subsequent events will be handled by the DrillDownEditor's listener
		eventBus.removeListener(AppEvents.DRILL_DOWN, initialDrillDownListener);

		layout();
	}

	
	@Override
	public void onUIAction(String actionId) {
		if (UIActions.REFRESH.equals(actionId)) {
			onRefresh();
		}
	}
	
	public void onRefresh() {
		final PivotTableReportElement element = createElement();
		service.execute(new GeneratePivotTable(element), getMonitor(),
				new AsyncCallback<PivotContent>() {
					public void onFailure(Throwable throwable) {
						MessageBox.alert(I18N.CONSTANTS.error(),
								I18N.CONSTANTS.errorOnServer(), null);
					}

					public void onSuccess(PivotContent content) {
						element.setContent((PivotContent) content);
						setContent(element);
					}
				});
	}

	public PivotTableReportElement createElement() {
		PivotTableReportElement table = new PivotTableReportElement();
		table.setRowDimensions(getRowStore().getModels());
		table.setColumnDimensions(getColStore().getModels());

		List<IndicatorDTO> selectedIndicators = getSelectedIndicators();
		for (IndicatorDTO indicator : selectedIndicators) {
			table.getFilter().addRestriction(DimensionType.Indicator,
					indicator.getId());
		}

		List<AdminEntityDTO> entities = getAdminRestrictions();
		for (AdminEntityDTO entity : entities) {
			table.getFilter().addRestriction(DimensionType.AdminLevel,
					entity.getId());
		}

		List<PartnerDTO> partners = getPartnerRestrictions();
		for (PartnerDTO entity : partners) {
			table.getFilter().addRestriction(DimensionType.Partner,
					entity.getId());
		}

		if (getMinDate() != null) {
			table.getFilter().setMinDate(getMinDate());
		}

		if (getMaxDate() != null) {
			table.getFilter().setMaxDate(getMaxDate());
		}
		return table;
	}

	public ListStore<Dimension> getRowStore() {
		return rowDims;
	}

	public ListStore<Dimension> getColStore() {
		return colDims;
	}

	public void setSchema(SchemaDTO result) {
		// indicatorPanel.setSchema(result);
	}

	public void enableUIAction(String actionId, boolean enabled) {
		Component button = gridToolBar.getItemByItemId(actionId);
		if (button != null) {
			button.setEnabled(enabled);
		}
	}

	public void setDimensionChecked(ModelData d, boolean checked) {
		treePanel.setChecked(d, checked);
	}

	public void setContent(PivotTableReportElement element) {
		gridContainer.setData(element);
	}

	public AsyncMonitor getMonitor() {
		return new MaskingAsyncMonitor(this, I18N.CONSTANTS.loading());
	}

	public List<IndicatorDTO> getSelectedIndicators() {
		return indicatorPanel.getSelection();
	}

	public List<AdminEntityDTO> getAdminRestrictions() {
		return adminPanel.getSelection();
	}

	public Date getMinDate() {
		return datePanel.getMinDate();
	}

	public Date getMaxDate() {
		return datePanel.getMaxDate();
	}

	public List<PartnerDTO> getPartnerRestrictions() {
		return partnerPanel.getSelection();
	}

	public TreeStore<ModelData> getDimensionStore() {
		return this.dimensionStore;
	}

	private String trim(String s) {
		if (s == null || "".equals(s)) {
			return "NO_NAME";
		}
		s = s.trim();
		if (s.length() > 20) {
			return s.substring(0, 19) + "...";
		} else {
			return s;
		}
	}

	private class Proxy implements DataProxy<List<ModelData>> {

		private SchemaDTO schema;

		@Override
		public void load(DataReader<List<ModelData>> listDataReader,
				final Object parent,
				final AsyncCallback<List<ModelData>> callback) {

			if (schema == null) {
				service.execute(new GetSchema(), getMonitor(),
						new AsyncCallback<SchemaDTO>() {
							@Override
							public void onFailure(Throwable caught) {
								callback.onFailure(caught);
							}

							@Override
							public void onSuccess(SchemaDTO result) {
								schema = result;
								loadChildren(parent, callback);
							}
						});
			} else {
				loadChildren(parent, callback);
			}
		}

		public void loadChildren(Object parent,
				final AsyncCallback<List<ModelData>> callback) {
			if (parent != null && parent instanceof DimensionFolder) {
				DimensionFolder folder = (DimensionFolder) parent;
				DimensionType type = folder.getType();
				final ArrayList<ModelData> dims = new ArrayList<ModelData>();

				if (type == DimensionType.Date) {
					// add time dimension
					int idSeq = 0;
					dims.add(new DateDimension(I18N.CONSTANTS.year(), idSeq++,
							DateUnit.YEAR, null));
					dims.add(new DateDimension(I18N.CONSTANTS.quarter(),
							idSeq++, DateUnit.QUARTER, null));
					dims.add(new DateDimension(I18N.CONSTANTS.month(), idSeq++,
							DateUnit.MONTH, null));

				} else if (type == DimensionType.AdminLevel) {
					// add geo dimensions
					for (CountryDTO country : schema.getCountries()) {
						for (AdminLevelDTO level : country.getAdminLevels()) {
							dims.add(new AdminDimension(level.getName(), level
									.getId()));
						}
					}

				} else if (type == DimensionType.AttributeGroup) {
					if (folder.getDepth() == 0) {
						// folders for database names
						for (UserDatabaseDTO db : schema.getDatabases()) {
							for (ActivityDTO act : db.getActivities()) {
								if (act.getAttributeGroups() != null
										&& act.getAttributeGroups().size() > 0) {
									dims.add(new DimensionFolder(db.getName(),
											DimensionType.AttributeGroup,
											folder.getDepth() + 1, db.getId()));
									break;
								}
							}
						}

					} else if (folder.getDepth() == 1) {
						// folders for activity names
						UserDatabaseDTO db = schema.getDatabaseById(folder
								.getId());
						for (ActivityDTO act : db.getActivities()) {
							if (act.getAttributeGroups() != null
									&& act.getAttributeGroups().size() > 0) {
								dims.add(new DimensionFolder(act.getName(),
										DimensionType.AttributeGroup, folder
												.getDepth() + 1, act.getId()));
								break;
							}
						}

					} else if (folder.getDepth() == 2) {
						// attribute groups
						ActivityDTO act = schema
								.getActivityById(folder.getId());
						for (AttributeGroupDTO attrGroup : act
								.getAttributeGroups()) {
							dims.add(new AttributeGroupDimension(attrGroup
									.getName(), attrGroup.getId()));
						}

					} else {
						assert false;
					}
				} else {
					assert false;
				}
				callback.onSuccess(dims);
			}
		}
	}

}