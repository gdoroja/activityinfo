package org.sigmah.client.page.entry;

import java.util.List;

import org.sigmah.client.dispatch.Dispatcher;
import org.sigmah.shared.command.Filter;
import org.sigmah.shared.command.GetAdminEntities;
import org.sigmah.shared.command.GetSchema;
import org.sigmah.shared.command.GetSites;
import org.sigmah.shared.command.result.AdminEntityResult;
import org.sigmah.shared.command.result.SiteResult;
import org.sigmah.shared.dto.AdminEntityDTO;
import org.sigmah.shared.dto.AdminLevelDTO;
import org.sigmah.shared.dto.CountryDTO;
import org.sigmah.shared.dto.SchemaDTO;
import org.sigmah.shared.report.model.DimensionType;

import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseTreeLoader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.data.SortInfo;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Loads sites grouped by a set of AdminLevels
 */
class SiteAdminTreeLoader extends BaseTreeLoader<ModelData> {

	private TreeProxy treeProxy;
	
	public SiteAdminTreeLoader(Dispatcher dispatcher) {
		super(new TreeProxy(dispatcher));
		treeProxy = (TreeProxy)proxy;
	}
	
	public void setAdminLeafLevelId(int leafLevelId) {
		treeProxy.setAdminLeafLevelId(leafLevelId);
	}
	
	public void setFilter(Filter filter) {
		treeProxy.setFilter(filter);
	}
	
	@Override
	public boolean hasChildren(ModelData parent) {
		return parent instanceof AdminEntityDTO;
	}
		
	private static class TreeProxy extends RpcProxy<List<ModelData>> {

		private final Dispatcher dispatcher;
		private List<Integer> adminLevelIds = null;
		private int leafLevelId;
		private Filter filter;
		
		public TreeProxy(Dispatcher dispatcher) {
			super();
			this.dispatcher = dispatcher;
		}

		public void setFilter(Filter filter) {
			this.filter = filter;
		}

		public void setAdminLeafLevelId(int leafLevelId) {
			this.leafLevelId = leafLevelId;
			this.adminLevelIds = null;
		}
		
		@Override
		protected void load(Object parentNode,
				AsyncCallback<List<ModelData>> callback) {
		
			if(adminLevelIds == null) {
				loadAdminLevels(parentNode, callback);
			} else {
				loadNodes(parentNode, callback);
			}
		}

		private void loadAdminLevels(final Object parentNode,
				final AsyncCallback<List<ModelData>> callback) {

			dispatcher.execute(new GetSchema(), null, new AsyncCallback<SchemaDTO>() {

				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}

				@Override
				public void onSuccess(SchemaDTO result) {
					try {
						initLevels(result);
						loadNodes(parentNode, callback);
					} catch(Exception e) {
						callback.onFailure(e);
					}
				}
			});
		}

		private void initLevels(SchemaDTO result) {
			CountryDTO country = result.getCountryByAdminLevelId(leafLevelId);
			adminLevelIds = Lists.newArrayList();
			for(AdminLevelDTO level : country.getAdminLevelAncestors(leafLevelId)) {
				adminLevelIds.add(level.getId());
			}
		}	

		private void loadNodes(Object parentNode,
				AsyncCallback<List<ModelData>> callback) {
			if(parentNode == null) {
				GetAdminEntities query = new GetAdminEntities(adminLevelIds.get(0));
				query.setFilter(filter);
				
				loadAdminEntities(query, callback);
				
			} else if(parentNode instanceof AdminEntityDTO) {
				AdminEntityDTO parentEntity = (AdminEntityDTO) parentNode;
				int depth = adminLevelIds.indexOf(parentEntity.getLevelId());
				if(depth+1 < adminLevelIds.size()) {
					loadChildAdminLevels(parentEntity, adminLevelIds.get(depth+1), callback);
				} else {
					loadSites(parentEntity, callback);
				}
			}
		}
		
		private void loadChildAdminLevels(AdminEntityDTO parentEntity,
				int adminLevelId,
				AsyncCallback<List<ModelData>> callback) {
			
			GetAdminEntities query = new GetAdminEntities();
			query.setLevelId(adminLevelId);
			query.setParentId(parentEntity.getId());
			query.setFilter(filter);
			
			loadAdminEntities(query, callback);
		}
		
		private void loadAdminEntities(GetAdminEntities query, final AsyncCallback<List<ModelData>> callback) {
			dispatcher.execute(query, null, new AsyncCallback<AdminEntityResult>() {

				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}

				@Override
				public void onSuccess(AdminEntityResult result) {
					callback.onSuccess((List)result.getData());
				}
			});
		}

		private void loadSites(AdminEntityDTO parentEntity,
				final AsyncCallback<List<ModelData>> callback) {

			Filter childFilter = new Filter(filter);
			childFilter.addRestriction(DimensionType.AdminLevel, parentEntity.getId());
			
			GetSites siteQuery = new GetSites();
			siteQuery.setFilter(childFilter);
			siteQuery.setSortInfo(new SortInfo("locationName", SortDir.ASC));
			
			dispatcher.execute(siteQuery, null, new AsyncCallback<SiteResult>() {

				@Override
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}

				@Override
				public void onSuccess(SiteResult result) {
					callback.onSuccess((List)result.getData());
				}
			});
		}
	}
}