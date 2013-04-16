/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.qpool.model;

import org.olat.core.id.Identity;
import org.olat.core.id.Roles;

/**
 * 
 * Initial date: 28.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SearchQuestionItemParams {
	
	private Long poolKey;
	private String format;
	private String searchString;
	private boolean favoritOnly;
	private Identity author;
	
	private final Identity identity;
	private final Roles roles;
	
	public SearchQuestionItemParams(Identity identity, Roles roles) {
		this.identity = identity;
		this.roles = roles;
	}

	public Long getPoolKey() {
		return poolKey;
	}

	public void setPoolKey(Long poolKey) {
		this.poolKey = poolKey;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public boolean isFavoritOnly() {
		return favoritOnly;
	}

	public void setFavoritOnly(boolean favoritOnly) {
		this.favoritOnly = favoritOnly;
	}

	public Identity getAuthor() {
		return author;
	}

	public void setAuthor(Identity author) {
		this.author = author;
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public Identity getIdentity() {
		return identity;
	}
	
	public Roles getRoles() {
		return roles;
	}
	
	@Override
	public SearchQuestionItemParams clone() {
		SearchQuestionItemParams clone = new SearchQuestionItemParams(identity, roles);
		clone.poolKey = poolKey;
		clone.format = format;
		clone.searchString = searchString;
		clone.favoritOnly = favoritOnly;
		clone.author = author;
		return clone;
	}
}
