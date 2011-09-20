/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.client.page.entry.editor;

import java.util.Map;

import org.sigmah.client.AppEvents;
import org.sigmah.client.EventBus;
import org.sigmah.client.dispatch.AsyncMonitor;
import org.sigmah.client.dispatch.Dispatcher;
import org.sigmah.client.event.SiteEvent;
import org.sigmah.client.page.common.toolbar.UIActions;
import org.sigmah.shared.command.CreateEntity;
import org.sigmah.shared.command.CreateSite;
import org.sigmah.shared.command.UpdateSite;
import org.sigmah.shared.command.result.CreateResult;
import org.sigmah.shared.command.result.VoidResult;
import org.sigmah.shared.dto.ActivityDTO;
import org.sigmah.shared.dto.AdminEntityDTO;
import org.sigmah.shared.dto.CountryDTO;
import org.sigmah.shared.dto.PartnerDTO;
import org.sigmah.shared.dto.ProjectDTO;
import org.sigmah.shared.dto.SiteDTO;
import org.sigmah.shared.util.mapping.BoundingBoxDTO;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.store.ListStore;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class SiteFormPresenter implements SiteFormLeash {

    public interface View {

        public void init(SiteFormPresenter presenter,
                         ActivityDTO activity,
                         ListStore<PartnerDTO> partnerStore,
                         ListStore<SiteDTO> assessmentStore,
                         ListStore<ProjectDTO> projectStore);

        public void setSite(SiteDTO site);

        public AdminFieldSetPresenter.View createAdminFieldSetView(ActivityDTO activity);

        public MapEditView createMapView(CountryDTO country);
        
        public boolean validate();

        public boolean isDirty();

        public Map<String, Object> getChanges();

        public Map<String, Object> getPropertyMap();

        public AsyncMonitor getMonitor();

        public void setActionEnabled(String actionId, boolean enabled);

        public void hide();
    }

    private final EventBus eventBus;
    private final Dispatcher service;

    private SiteDTO currentSite;
    private ActivityDTO currentActivity;

    protected MapPresenter mapPresenter;
    protected AdminFieldSetPresenter adminPresenter;
    private View view;

    public SiteFormPresenter(EventBus eventBus, Dispatcher service, ActivityDTO activity, View view) {
        super();
        this.eventBus = eventBus;
        this.service = service;
        this.view = view;

        init(activity);
    }


    protected ListStore<PartnerDTO> createPartnerStore() {
        ListStore<PartnerDTO> store = new ListStore<PartnerDTO>();

        if (currentActivity.getDatabase().isEditAllAllowed()) {

            for (PartnerDTO partner : currentActivity.getDatabase().getPartners()) {
                if (partner.isOperational()) {
                    store.add(partner);
                }
            }
            
            store.sort("name", Style.SortDir.ASC);

        } else {
            store.add(currentActivity.getDatabase().getMyPartner());
        }
        return store;
    }
    
    protected ListStore<ProjectDTO> createProjectStore() {
    	 ListStore<ProjectDTO> result = new ListStore<ProjectDTO>();
    	 
    	 result.add(currentActivity.getDatabase().getProjects());
    	 result.sort("name", Style.SortDir.ASC);
    	 
    	 return result;
    }

	protected ListStore<SiteDTO> createAsssessmentStore() {
        return new ListStore<SiteDTO>();
    }

    public void init(ActivityDTO activity) {

        this.currentActivity = activity;

        mapPresenter = new MapPresenter(view.createMapView(activity.getDatabase().getCountry()));

        adminPresenter = new AdminFieldSetPresenter(service, currentActivity, view.createAdminFieldSetView(currentActivity));
        adminPresenter.setListener(new AdminFieldSetPresenter.Listener() {
            @Override
            public void onBoundsChanged(String name, BoundingBoxDTO bounds) {
                mapPresenter.setBounds(name, bounds);
            }

            @Override
            public void onModified() {
                SiteFormPresenter.this.onModified();
            }
        });
        

        view.init(this, currentActivity, createPartnerStore(), createAsssessmentStore(), createProjectStore());
    }

    public void setSite(SiteDTO site) {
        currentSite = site;

        adminPresenter.setSite(site);
        mapPresenter.setSite(site, adminPresenter.getBoundsName(), adminPresenter.getBounds());
        
        view.setSite(site);
        view.setActionEnabled(UIActions.save, false);
    }

    public void newSite() {
        setSite(new SiteDTO());
    }

    public void onUIAction(String actionId) {

        if (UIActions.save.equals(actionId)) {
            onSave();
        } else if (UIActions.cancel.equals(actionId)) {

        }
    }

    public void onModified() {
        view.setActionEnabled(UIActions.save, view.isDirty() || adminPresenter.isDirty());
    }
    
    private void addProjectIdToMap(Map<String, Object> map) {
    	ProjectDTO project = (ProjectDTO)map.get("project");
    	if(project != null) {
	        map.put("projectId", project.getId());
	        map.remove("project");
    	}
    }

    public void onSave() {

        if (currentSite.hasId()) {

            final Map<String, Object> changes = view.getChanges();
            if (adminPresenter.isDirty()) {
                changes.putAll(adminPresenter.getChangeMap());
            }
            
            // The projectId will always be updated, regardless of change/create mode
            addProjectIdToMap(changes);

            service.execute(new UpdateSite(currentSite.getId(), changes), view.getMonitor(), new AsyncCallback<VoidResult>() {
                @Override
                public void onFailure(Throwable throwable) {
                    // let monitor display
                }

                @Override
                public void onSuccess(VoidResult voidResult) {
                    updateSiteModel();

                    eventBus.fireEvent(new SiteEvent(AppEvents.SiteChanged, SiteFormPresenter.this, currentSite));
                    view.hide();
                }


            });
        } else {

            final Map<String, Object> properties = view.getPropertyMap();
            properties.putAll(adminPresenter.getChangeMap());
            properties.put("activityId", currentActivity.getId());
            addProjectIdToMap(properties);
            
            // hack: we need to send partnerId instead of partner.id, but the
            // nonsense form binding that i set up here doesn't support custom bindings
            properties.put("partnerId", ((PartnerDTO)properties.get("partner")).getId());
            properties.remove("partner");

            service.execute(new CreateSite(properties), view.getMonitor(), new AsyncCallback<CreateResult>() {
                @Override
                public void onFailure(Throwable throwable) {
                    // TODO: handle failure
                }

                @Override
                public void onSuccess(CreateResult result) {
                	currentSite.setId(result.getNewId());
                    updateSiteModel();

                    eventBus.fireEvent(new SiteEvent(AppEvents.SiteCreated, SiteFormPresenter.this, currentSite));
                    view.hide();
                }
            });
        }
    }
    
	private void updateSiteModel() {
		for (Map.Entry<String, Object> change : view.getChanges().entrySet()) {
            currentSite.set(change.getKey(), change.getValue());
        }
        for (Map.Entry<String, AdminEntityDTO> entity : adminPresenter.getPropertyMap().entrySet()) {
        	currentSite.set(entity.getKey(), entity.getValue());
        }
	}
    
    public int getActivityId() {
        return currentActivity.getId();
    }

    public void destroy() {

    }
}
