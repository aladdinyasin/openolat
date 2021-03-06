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
package org.olat.core.id;

import org.junit.Test;
import org.olat.core.logging.AssertException;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class IdentityEnvironmentTest {
	
	/**
	 * Can set several times the roles if they are the same
	 */
	@Test
	public void testSetRoles() {
		IdentityEnvironment env = new IdentityEnvironment();
		env.setRoles(new Roles(true, true, true, true, true, true, true));
		env.setRoles(new Roles(true, true, true, true, true, true, true));
	}
	
	/**
	 * Cannot set roles with different permissions
	 */
	@Test(expected=AssertException.class)
	public void testSetDifferentRoles() {
		IdentityEnvironment env = new IdentityEnvironment();
		env.setRoles(new Roles(true, true, true, true, true, true, true));
		env.setRoles(new Roles(true, false, false, false, false, false, false));
	}

}
