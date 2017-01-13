/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.log.localprovider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A convenience wrapper for a projection map. Makes it easier to create and use projection maps.
 */
/* package */class ProjectionMap extends HashMap<String, String> {
	private static final long serialVersionUID = -4004367756025538190L;

	public static class Builder {

		private ProjectionMap mMap = new ProjectionMap();

		public Builder add(String column) {
			mMap.putColumn(column, column);
			return this;
		}

		public Builder add(String alias, String expression) {
			mMap.putColumn(alias, expression + " AS " + alias);
			return this;
		}

		public Builder addAll(String[] columns) {
			for (String column : columns) {
				add(column);
			}
			return this;
		}

		public Builder addAll(ProjectionMap map) {
			for (Map.Entry<String, String> entry : map.entrySet()) {
				mMap.putColumn(entry.getKey(), entry.getValue());
			}
			return this;
		}

		public ProjectionMap build() {
			String[] columns = new String[mMap.size()];
			mMap.keySet().toArray(columns);
			Arrays.sort(columns);
			mMap.mColumns = columns;
			return mMap;
		}

	}

	private String[] mColumns;

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Returns a sorted array of all column names in the projection map.
	 */
	public String[] getColumnNames() {
		return mColumns;
	}

	private void putColumn(String alias, String column) {
		super.put(alias, column);
	}

	@Override
	public String put(String key, String value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends String, ? extends String> map) {
		throw new UnsupportedOperationException();
	}
}
