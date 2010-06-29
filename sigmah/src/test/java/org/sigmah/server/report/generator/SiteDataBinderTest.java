package org.sigmah.server.report.generator;

import com.google.inject.Inject;
import org.hibernate.criterion.Restrictions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sigmah.server.dao.OnDataSet;
import org.sigmah.server.dao.SiteTableDAO;
import org.sigmah.server.dao.hibernate.SiteTableDAOHibernate;
import org.sigmah.server.domain.SiteData;
import org.sigmah.server.domain.User;
import org.sigmah.test.InjectionSupport;
import org.sigmah.test.MockHibernateModule;
import org.sigmah.test.Modules;

import javax.persistence.EntityManager;
import java.util.List;

@RunWith(InjectionSupport.class)
@Modules({MockHibernateModule.class})
@OnDataSet("/dbunit/sites-simple1.db.xml")
public class SiteDataBinderTest {

    @Inject
    private EntityManager em;

    @Inject
    private SiteTableDAOHibernate dao;

    @Test
    public void test() {

        List<SiteData> sites = dao.query(new User(), Restrictions.eq("site.id", 2),
                null, new SiteDataBinder(), SiteTableDAO.RETRIEVE_ALL, 0, -1);

        Assert.assertEquals("sites returned", 1, sites.size());

        SiteData site = sites.get(0);

        Assert.assertEquals("Ngshwe", site.getLocationName());
        Assert.assertNull(site.getLocationAxe());
        Assert.assertTrue("has coords", site.hasLatLong());
        Assert.assertEquals(1.323, site.getLongitude(), 0.001);
        Assert.assertEquals(28.232, site.getLatitude(), 0.001);
        Assert.assertEquals("partner", "NRC", site.getPartnerName());
        Assert.assertEquals("indicator 1", 3600, site.getIndicatorValue(1).intValue());
        Assert.assertEquals("indicator 2", 1200, site.getIndicatorValue(2).intValue());
    }
}