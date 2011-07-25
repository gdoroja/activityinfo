/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.shared.report.model;

import org.sigmah.client.page.map.layerOptions.PiechartLayerOptions;
import org.sigmah.shared.map.BaseMap;
import org.sigmah.shared.map.GoogleBaseMap;
import org.sigmah.shared.report.content.MapContent;
import org.sigmah.shared.report.model.layers.AbstractMapLayer;
import org.sigmah.shared.report.model.layers.BubbleMapLayer;
import org.sigmah.shared.report.model.layers.IconMapLayer;
import org.sigmah.shared.report.model.layers.MapLayer;
import org.sigmah.shared.report.model.layers.PiechartMapLayer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Models a map element within a report
 */
public class MapReportElement extends ReportElement<MapContent> {
    private String baseMapId = BaseMap.getDefaultMapId();
    private int width = 640;
    private int height = 480;
    private List<MapLayer> layers = new ArrayList<MapLayer>(0);

    public MapReportElement() {
    }

    @XmlElement(name="tileBaseMap", required = true)
    public String getBaseMapId() {
        return baseMapId;
    }

    public void setBaseMapId(String baseMapId) {
        this.baseMapId = baseMapId;
    }

    @XmlElement(defaultValue="640")
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @XmlElement(defaultValue="480")
    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void addLayer(MapLayer layer) {
        this.layers.add(layer);
    }

    @XmlElementWrapper(name="layers")
    @XmlElements({
        @XmlElement(name="bubbles", type=BubbleMapLayer.class),
        @XmlElement(name="pie", type=PiechartMapLayer.class),
            @XmlElement(name="icons", type=IconMapLayer.class)
    })
    public List<MapLayer> getLayers() {
        return layers;
    }

    public void setLayers(List<MapLayer> layers) {
        this.layers = layers;
    }
    
    public void setLayers(MapLayer... layers) {
    	this.layers = new ArrayList<MapLayer>();
    	this.layers.addAll(Arrays.asList(layers));
    }
}