package org.sigmah.client.page.entry.form;

import org.sigmah.client.dispatch.Dispatcher;
import org.sigmah.client.page.entry.location.LocationDialog;
import org.sigmah.shared.command.Filter;
import org.sigmah.shared.command.GetSchema;
import org.sigmah.shared.dto.ActivityDTO;
import org.sigmah.shared.dto.LocationDTO;
import org.sigmah.shared.dto.SchemaDTO;
import org.sigmah.shared.dto.SiteDTO;
import org.sigmah.shared.report.model.DimensionType;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class SiteDialogLauncher {

	private final Dispatcher dispatcher;
	
	public SiteDialogLauncher(Dispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
	}


	public void addSite(final Filter filter, final SiteDialogCallback callback) {
		if(filter.isDimensionRestrictedToSingleCategory(DimensionType.Activity)) {
			dispatcher.execute(new GetSchema(), null, new AsyncCallback<SchemaDTO>() {

				@Override
				public void onFailure(Throwable arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onSuccess(SchemaDTO schema) {
					final ActivityDTO activity = schema.getActivityById(
							filter.getRestrictedCategory(DimensionType.Activity));
					
					Log.trace("adding site for activity " + activity + ", locationType = " + activity.getLocationType());
					 
					if(activity.getLocationType().isAdminLevel()) {
						addNewSiteWithBoundLocation(activity);
					} else {
						chooseLocationThenAddSite(activity, callback);
					}
				}
			});
		}
	}
	
	public void editSite(final SiteDTO site, final SiteDialogCallback callback) {
		dispatcher.execute(new GetSchema(), null, new AsyncCallback<SchemaDTO>() {

			@Override
			public void onFailure(Throwable caught) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSuccess(SchemaDTO schema) {
				ActivityDTO activity = schema.getActivityById(site.getActivityId());
				SiteDialog dialog = new SiteDialog(dispatcher, activity);
				dialog.showExisting(site, callback);
			}
		});
	}


	private void chooseLocationThenAddSite(final ActivityDTO activity, final SiteDialogCallback callback) {
		LocationDialog dialog = new LocationDialog(dispatcher, activity.getDatabase().getCountry(),
				activity.getLocationType());
		
		dialog.show(new LocationDialog.Callback() {
			
			@Override
			public void onSelected(LocationDTO location, boolean isNew) {
				SiteDTO newSite = new SiteDTO();
				newSite.setActivityId(activity.getId());
				newSite.setLocation(location);
				
				SiteDialog dialog = new SiteDialog(dispatcher, activity);
				dialog.showNew(newSite, location, isNew, callback);
			}
		});		
	}
	
	private void addNewSiteWithBoundLocation(ActivityDTO activity) {
		SiteDTO newSite = new SiteDTO();
		newSite.setActivityId(activity.getId());
		
		SiteDialog dialog = new SiteDialog(dispatcher, activity);
		dialog.show();
	}
}
