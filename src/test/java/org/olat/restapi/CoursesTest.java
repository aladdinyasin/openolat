/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/


package org.olat.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.course.ICourse;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.manager.RepositoryEntryLifecycleDAO;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.restapi.support.vo.CourseVOes;
import org.olat.test.OlatJerseyTestCase;
import org.springframework.beans.factory.annotation.Autowired;

public class CoursesTest extends OlatJerseyTestCase {
	
	private static final OLog log = Tracing.createLoggerFor(CoursesTest.class);
	
	private Identity admin;
	private ICourse course1, course2, course3;
	private RepositoryEntry re1, re2, re3;
	private String externalId, externalRef;
	private String externalId3;
	private RestConnection conn;
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryEntryLifecycleDAO reLifecycleDao;

	/**
	 * SetUp is called before each test.
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		conn = new RestConnection();
		try {
			// create course and persist as OLATResourceImpl
			admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			course1 = CoursesWebService.createEmptyCourse(admin, "courses1", "courses1 long name", null, null, RepositoryEntry.ACC_OWNERS, false, null, null, null, null);
			
			externalId = UUID.randomUUID().toString();
			externalRef = UUID.randomUUID().toString();
			course2 = CoursesWebService.createEmptyCourse(admin, "courses2", "courses2 long name", null, null, RepositoryEntry.ACC_OWNERS, false, externalId, externalRef, "all", null);
			
			dbInstance.commitAndCloseSession();
			
			re1 = repositoryManager.lookupRepositoryEntry(course1, false);
			re2 = repositoryManager.lookupRepositoryEntry(course2, false);

			externalId3 = UUID.randomUUID().toString();
			course3 = CoursesWebService.createEmptyCourse(admin, "courses3", "courses3 long name", null, null, RepositoryEntry.ACC_OWNERS, false, externalId3, null, "all", null);
			re3 = repositoryManager.lookupRepositoryEntry(course3, false);
			RepositoryEntryLifecycle lifecycle3 = reLifecycleDao.create("course3 lifecycle", UUID.randomUUID().toString(), true, new Date(), new Date());
			dbInstance.commit();
			re3.setLifecycle(lifecycle3);
			re3 = dbInstance.getCurrentEntityManager().merge(re3);
			dbInstance.commitAndCloseSession();
		} catch (Exception e) {
			log.error("Exception in setUp(): " + e);
		}
	}
	
	@After
	public void tearDown() throws Exception {
		try {
			if(conn != null) {
				conn.shutdown();
			}
		} catch (Exception e) {
			log.error("", e);
			throw e;
		}
	}
	
	@Test
	public void testGetCourses() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/repo/courses").build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		assertNotNull(courses);
		assertTrue(courses.size() >= 2);
		
		boolean vo1 = false;
		boolean vo2 = false;
		for(CourseVO course:courses) {
			Long repoEntryKey = course.getRepoEntryKey();
			if(repoEntryKey != null && re1.getKey().equals(repoEntryKey)) {
				vo1 = true;
				Assert.assertEquals("courses1", course.getTitle());
			}
				
			if(repoEntryKey != null && re2.getKey().equals(repoEntryKey)) {
				vo2 = true;
				Assert.assertEquals("courses2", course.getTitle());
			}	
		}
		Assert.assertTrue(vo1);
		Assert.assertTrue(vo2);
	}
	
	@Test
	public void testGetCourses_searchExternalID() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/repo/courses").queryParam("externalId", externalId).build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		assertNotNull(courses);
		assertTrue(courses.size() >= 1);
		
		CourseVO vo = null;
		for(CourseVO course:courses) {
			if(externalId.equals(course.getExternalId())) {
				vo = course;
			}
		}
		assertNotNull(vo);
		assertEquals(vo.getKey(), course2.getResourceableId());
	}
	
	@Test
	public void testGetCourses_searchExternalID_withLifecycle() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/repo/courses").queryParam("externalId", externalId3).build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		
		List<CourseVO> courses = parseCourseArray(body);
		assertNotNull("Course list cannot be null", courses);
		assertEquals(1, courses.size());
		CourseVO vo = courses.get(0);
		assertNotNull("Course cannot be null", vo);
		assertEquals(vo.getKey(), course3.getResourceableId());
		assertNotNull("Has a lifecycle", vo.getLifecycle());
		assertNotNull("Life cycle has a soft key", vo.getLifecycle().getSoftkey());
	}
	
	@Test
	public void testGetCourses_searchExternalRef() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/repo/courses").queryParam("externalRef", externalRef).build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		assertNotNull(courses);
		assertTrue(courses.size() >= 1);
		
		CourseVO vo = null;
		for(CourseVO course:courses) {
			if(externalRef.equals(course.getExternalRef())) {
				vo = course;
			}
		}
		assertNotNull(vo);
		assertEquals(vo.getKey(), course2.getResourceableId());
	}
	
	@Test
	public void testGetCourses_managed() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/repo/courses").queryParam("managed", "true").build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		assertNotNull(courses);
		assertTrue(courses.size() >= 1);
		
		for(CourseVO course:courses) {
			boolean managed = StringHelper.containsNonWhitespace(course.getManagedFlags());
			Assert.assertTrue(managed);
		}
	}
	
	@Test
	public void testGetCourses_notManaged() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("/repo/courses").queryParam("managed", "false").build();
		HttpGet method = conn.createGet(request, MediaType.APPLICATION_JSON, true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		InputStream body = response.getEntity().getContent();
		List<CourseVO> courses = parseCourseArray(body);
		assertNotNull(courses);
		assertTrue(courses.size() >= 1);
		
		for(CourseVO course:courses) {
			boolean managed = StringHelper.containsNonWhitespace(course.getManagedFlags());
			Assert.assertFalse(managed);
		}
	}
	
	@Test
	public void testGetCoursesWithPaging() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
				.queryParam("start", "0").queryParam("limit", "1").build();
		HttpGet method = conn.createGet(uri, MediaType.APPLICATION_JSON + ";pagingspec=1.0", true);
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		CourseVOes courses = conn.parse(response, CourseVOes.class);
		assertNotNull(courses);
		assertNotNull(courses.getCourses());
		assertEquals(1, courses.getCourses().length);
	}
	
	@Test
	public void testCreateEmptyCourse() throws IOException, URISyntaxException {
		assertTrue(conn.login("administrator", "openolat"));
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
			.queryParam("shortTitle", "course3").queryParam("title", "course3 long name").build();
		HttpPut method = conn.createPut(uri, MediaType.APPLICATION_JSON, true);
		
		HttpResponse response = conn.execute(method);
		assertEquals(200, response.getStatusLine().getStatusCode());
		CourseVO course = conn.parse(response, CourseVO.class);
		assertNotNull(course);
		assertEquals("course3", course.getTitle());
		//check repository entry
		RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(course.getRepoEntryKey());
		assertNotNull(re);
		assertNotNull(re.getOlatResource());
	}
	
	@Test
	public void testImportCourse() throws IOException, URISyntaxException {
		URL cpUrl = CoursesTest.class.getResource("Course_with_blog.zip");
		assertNotNull(cpUrl);
		File cp = new File(cpUrl.toURI());

		assertTrue(conn.login("administrator", "openolat"));
		
		URI request = UriBuilder.fromUri(getContextURI()).path("repo/courses").build();
		HttpPost method = conn.createPost(request, MediaType.APPLICATION_JSON);

		String softKey = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
		HttpEntity entity = MultipartEntityBuilder.create()
				.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
				.addBinaryBody("file", cp, ContentType.APPLICATION_OCTET_STREAM, cp.getName())
				.addTextBody("filename", "Very_small_course.zip")
				.addTextBody("foldername", "New folder 1 2 3")
				.addTextBody("resourcename", "Very small course")
				.addTextBody("displayname", "Very small course")
				.addTextBody("access", "3")
				.addTextBody("softkey", softKey)
				.build();
		method.setEntity(entity);
		
		HttpResponse response = conn.execute(method);
		assertTrue(response.getStatusLine().getStatusCode() == 200 || response.getStatusLine().getStatusCode() == 201);
		
		CourseVO vo = conn.parse(response, CourseVO.class);
		assertNotNull(vo);
		assertNotNull(vo.getRepoEntryKey());
		assertNotNull(vo.getKey());
		
		Long repoKey = vo.getRepoEntryKey();
		RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(repoKey);
		assertNotNull(re);
		assertNotNull(re.getOlatResource());
		assertEquals("Very small course", re.getDisplayname());
		assertEquals(softKey, re.getSoftkey());
	}
	
	protected List<CourseVO> parseCourseArray(InputStream body) {
		try {
			ObjectMapper mapper = new ObjectMapper(jsonFactory); 
			return mapper.readValue(body, new TypeReference<List<CourseVO>>(){/* */});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}