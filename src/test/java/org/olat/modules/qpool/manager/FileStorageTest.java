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
package org.olat.modules.qpool.manager;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 11.03.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class FileStorageTest extends OlatTestCase {
	
	@Autowired
	private QPoolFileStorage qpoolFileStorage;
	
	@Test
	public void testGenerateDir() {
		String uuid = UUID.randomUUID().toString();
		String dir = qpoolFileStorage.generateDir(uuid);
		Assert.assertNotNull(dir);
		VFSContainer container = qpoolFileStorage.getContainer(dir);
		Assert.assertTrue(container.exists());
	}

	/**
	 * With the same uuid, generate 2 different directories
	 */
	@Test
	public void testGenerateDir_testUnicity() {
		String fakeUuid = "aabbccddeeff";
		String dir1 = qpoolFileStorage.generateDir(fakeUuid);
		String dir2 = qpoolFileStorage.generateDir(fakeUuid);
		
		//check
		Assert.assertNotNull(dir1);
		Assert.assertNotNull(dir2);
		Assert.assertFalse(dir1.equals(dir2));
	}
	
	@Test
	public void testDeleteDir() {
		String uuid = UUID.randomUUID().toString();
		String dir = qpoolFileStorage.generateDir(uuid);
		VFSContainer container = qpoolFileStorage.getContainer(dir);
		container.createChildLeaf("abc.txt");
		container.createChildLeaf("xyzc.txt");
		Assert.assertTrue(container.getItems().size() > 0);

		qpoolFileStorage.deleteDir(dir);
		
		Assert.assertTrue(container.getParentContainer().resolve(container.getName()) == null);
	}
}
