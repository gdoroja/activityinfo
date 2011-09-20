/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.server.sync;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.json.JSONException;
import org.sigmah.server.dao.hibernate.HibernateDAOProvider;
import org.sigmah.server.domain.DomainFilters;
import org.sigmah.shared.command.GetSyncRegionUpdates;
import org.sigmah.shared.command.result.SyncRegionUpdate;
import org.sigmah.shared.command.result.UserFavorites;
import org.sigmah.shared.dao.UserDatabaseDAO;
import org.sigmah.shared.domain.Activity;
import org.sigmah.shared.domain.AdminLevel;
import org.sigmah.shared.domain.Attribute;
import org.sigmah.shared.domain.AttributeGroup;
import org.sigmah.shared.domain.Country;
import org.sigmah.shared.domain.Indicator;
import org.sigmah.shared.domain.LocationType;
import org.sigmah.shared.domain.LockedPeriod;
import org.sigmah.shared.domain.OrgUnit;
import org.sigmah.shared.domain.Project;
import org.sigmah.shared.domain.User;
import org.sigmah.shared.domain.UserDatabase;
import org.sigmah.shared.domain.UserPermission;

import com.bedatadriven.rebar.sync.server.JpaUpdateBuilder;
import com.google.inject.Inject;

public class SchemaUpdateBuilder implements UpdateBuilder {

    private final UserDatabaseDAO userDatabaseDAO;
    private final EntityManager entityManager;

    private Set<Integer> countryIds = new HashSet<Integer>();
    private List<Country> countries = new ArrayList<Country>();
    private List<AdminLevel> adminLevels = new ArrayList<AdminLevel>();

    private List<UserDatabase> databases = new ArrayList<UserDatabase>();

    private Set<Integer> partnerIds = new HashSet<Integer>();
    private List<OrgUnit> partners = new ArrayList<OrgUnit>();

    private List<Activity> activities = new ArrayList<Activity>();
    private List<Indicator> indicators = new ArrayList<Indicator>();
    
    private Set<Integer> attributeGroupIds = new HashSet<Integer>();
    private List<AttributeGroup> attributeGroups = new ArrayList<AttributeGroup> ();
    private List<Attribute> attributes = new ArrayList<Attribute>();
    
    private Set<Integer> userIds = new HashSet<Integer>();
    private List<User> users = new ArrayList<User>();
    private List<LocationType> locationTypes  = new ArrayList<LocationType>();
    private List<UserPermission> userPermissions;

    private Class[] schemaClasses = new Class[] {
            Country.class,
            AdminLevel.class,
            LocationType.class,
            UserDatabase.class,
            OrgUnit.class,
            Activity.class,
            Indicator.class,
            AttributeGroup.class,
            Attribute.class,
            User.class,
            UserPermission.class,
            LockedPeriod.class,
            Project.class
    };
	private List<LockedPeriod> allLockedPeriods = new ArrayList<LockedPeriod>();
	private List<Project> projects = new ArrayList<Project>();

    @Inject
    public SchemaUpdateBuilder(EntityManagerFactory entityManagerFactory) {
        // create a new, unfiltered entity manager so we can see deleted records
        this.entityManager = entityManagerFactory.createEntityManager();
        this.userDatabaseDAO = HibernateDAOProvider.makeImplementation(UserDatabaseDAO.class, UserDatabase.class, 
        		entityManager);
    }

    public SyncRegionUpdate build(User user, GetSyncRegionUpdates request) throws JSONException {
        
    	try {
	    	// get the permissions before we apply the filter
	    	// otherwise they will be excluded
	    	userPermissions = entityManager.createQuery("select p from UserPermission p where p.user.id = ?1")
	                .setParameter(1, user.getId())
	                .getResultList();
	
	        DomainFilters.applyUserFilter(user, entityManager);
	        
	    	databases = userDatabaseDAO.queryAllUserDatabasesAlphabetically();
	    	
	    	
	        long localVersion = request.getLocalVersion() == null ? 0 : Long.parseLong(request.getLocalVersion());
	        long serverVersion = getCurrentSchemaVersion(user);
	
	        SyncRegionUpdate update = new SyncRegionUpdate();
	        update.setVersion(Long.toString(serverVersion));
	        update.setComplete(true);
	
	        if(localVersion == serverVersion) {
	            update.setComplete(true);
	        } else {
	            makeEntityLists(null);
	            update.setSql(buildSql());
	        }
	        return update;
    	} finally {
    		entityManager.close();
    	}
    }

    private String buildSql() throws JSONException {
        JpaUpdateBuilder builder = new JpaUpdateBuilder();
        for(Class schemaClass : schemaClasses) {
            builder.createTableIfNotExists(schemaClass);
            builder.deleteAll(schemaClass);
        }

        builder.insert(Country.class, countries);
        builder.insert(AdminLevel.class, adminLevels);
        builder.insert(UserDatabase.class, databases);
        builder.insert(OrgUnit.class, partners);
        builder.insert(Activity.class, activities);
        builder.insert(Indicator.class, indicators);
        builder.insert(AttributeGroup.class, attributeGroups);
        builder.insert(Attribute.class, attributes);
        builder.insert(LocationType.class, locationTypes);
        builder.insert(User.class, users);
        builder.insert(UserPermission.class, userPermissions);
        builder.insert(Project.class, projects);
        builder.insert(LockedPeriod.class, allLockedPeriods);

        builder.executeStatement("create table if not exists PartnerInDatabase (DatabaseId integer, PartnerId int)");
        builder.beginPreparedStatement("insert into PartnerInDatabase (DatabaseId, PartnerId) values (?, ?) ");
        for(UserDatabase db : databases) {
            for(OrgUnit orgUnit : db.getPartners()) {
                builder.addExecution(db.getId(), orgUnit.getId());
            }
        }
        builder.finishPreparedStatement();

        builder.executeStatement("create table if not exists AttributeGroupInActivity (ActivityId integer, AttributeGroupId integer)");
        builder.beginPreparedStatement("insert into AttributeGroupInActivity (ActivityId, AttributeGroupId) values (?,?)");
        for(UserDatabase db : databases) {
            for(Activity activity : db.getActivities()) {
                for(AttributeGroup group : activity.getAttributeGroups()) {
                    builder.addExecution(activity.getId(), group.getId());
                }
            }
        }
        builder.finishPreparedStatement();

        return builder.asJson();
    }

    private void makeEntityLists(User user) {
        for(UserDatabase database : databases) {
        	if(!userIds.contains(database.getOwner().getId())) {
        		User u = database.getOwner();
        		// don't send hashed password to client
// EEK i think hibernate will persist this to the database automatically if we change it here!!
//        		u.setHashedPassword("");
        		users.add(u);
        		userIds.add(u.getId());
        	}
            
            if(!countryIds.contains(database.getCountry().getId())) {
                countries.add(database.getCountry());
                adminLevels.addAll(database.getCountry().getAdminLevels());
                countryIds.add(database.getCountry().getId());
                for (org.sigmah.shared.domain.LocationType l: database.getCountry().getLocationTypes()) {
                	locationTypes.add(l);
                }
            }
            for(OrgUnit partner : database.getPartners()) {
                if(!partnerIds.contains(partner.getId())) {
                    partners.add(partner);
                    partnerIds.add(partner.getId());
                }
            }

            projects.addAll(new ArrayList<Project>(database.getProjects()));
            
           	allLockedPeriods.addAll(database.getLockedPeriods());
           	for (Project project : database.getProjects()) {
           		allLockedPeriods.addAll(project.getLockedPeriods());
           	}
            
            for(Activity activity : database.getActivities()) {
                allLockedPeriods.addAll(activity.getLockedPeriods());
                
                activities.add(activity);
                for(Indicator indicator : activity.getIndicators()) {
                    indicators.add(indicator);
                }
                for(AttributeGroup g: activity.getAttributeGroups()) {
                	if (!attributeGroupIds.contains(g.getId())) {
                		attributeGroups.add(g);
                        attributeGroupIds.add(g.getId());
                		for (Attribute a: g.getAttributes()) {
                			attributes.add(a);
                		}
                	}
                }
            }
            
        }
    }

    public long getCurrentSchemaVersion(User user) {
        long currentVersion = 1;
        for(UserDatabase db : databases) {
            if(db.getLastSchemaUpdate().getTime() > currentVersion) {
                currentVersion = db.getLastSchemaUpdate().getTime();
            }

            if(db.getOwner().getId() != user.getId()) {
                UserPermission permission = db.getPermissionByUser(user);
                if(permission.getLastSchemaUpdate().getTime() > permission.getId()) {
                    currentVersion = permission.getLastSchemaUpdate().getTime();
                }
            }
        }
        return currentVersion;
    }
}
