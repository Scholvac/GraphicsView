package de.sos.gv.gt.examples;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import de.sos.gv.gta.FeatureReader;

public class ShapeFileReader {


	public static void main(final String[] args) throws IOException {
		final File file = new File("src/test/resources/Germany/gadm40_DEU_1.shp");
		final Map<String, Object> map = new HashMap<>();
		map.put("url", file.toURI().toURL());

		final DataStore dataStore = DataStoreFinder.getDataStore(map);
		final String typeName = dataStore.getTypeNames()[0];

		final FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
		final Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")

		final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
		try (FeatureIterator<SimpleFeature> features = collection.features()) {
			while (features.hasNext()) {
				final SimpleFeature feature = features.next();
				FeatureReader.createSimpleItem(feature);
				System.out.println(feature.getID());
				final Object geom = feature.getDefaultGeometry();
				System.out.print(": ");
				System.out.println(feature.getDefaultGeometryProperty().getValue());
			}
		}
	}
}
