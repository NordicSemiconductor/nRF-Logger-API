/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.log.localprovider;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A convenience wrapper for a projection map. Makes it easier to create and use projection maps.
 */
@SuppressWarnings("unused")
/* package */class ProjectionMap extends HashMap<String, String> {
	private static final long serialVersionUID = -4004367756025538190L;

	public static class Builder {

		private ProjectionMap mMap = new ProjectionMap();

		Builder add(@NonNull String column) {
			mMap.putColumn(column, column);
			return this;
		}

		@SuppressWarnings("SameParameterValue")
		Builder add(@NonNull String alias, @NonNull String expression) {
			mMap.putColumn(alias, expression + " AS " + alias);
			return this;
		}

		Builder addAll(@NonNull String[] columns) {
			for (String column : columns) {
				add(column);
			}
			return this;
		}

		Builder addAll(@NonNull ProjectionMap map) {
			for (Map.Entry<String, String> entry : map.entrySet()) {
				mMap.putColumn(entry.getKey(), entry.getValue());
			}
			return this;
		}

		ProjectionMap build() {
			String[] columns = new String[mMap.size()];
			mMap.keySet().toArray(columns);
			Arrays.sort(columns);
			mMap.mColumns = columns;
			return mMap;
		}

	}

	private String[] mColumns;

	private ProjectionMap() {
		// empty private constructor
	}

	static Builder builder() {
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
	public void putAll(@NonNull Map<? extends String, ? extends String> map) {
		throw new UnsupportedOperationException();
	}
}
