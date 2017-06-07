/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.olat.core.commons.services.webdav.servlets;

import java.io.InputStream;

/**
 * Represents a file or directory within a web application. It borrows heavily
 * from {@link java.io.File}.
 */
public interface WebResource {
	
	String getPath();
	
    /**
     * See {@link java.io.File#lastModified()}.
     */
    long getLastModified();

    /**
     * Return the last modified time of this resource in the correct format for
     * the HTTP Last-Modified header as specified by RFC 2616.
     */
    String getLastModifiedHttp();

    /**
     * See {@link java.io.File#exists()}.
     */
    boolean exists();

    /**
     * See {@link java.io.File#isDirectory()}.
     */
    boolean isDirectory();

    /**
     * See {@link java.io.File#isFile()}.
     */
    boolean isFile();

    /**
     * See {@link java.io.File#delete()}.
     */
    //boolean delete();

    /**
     * See {@link java.io.File#getName()}.
     */
    String getName();

    /**
     * See {@link java.io.File#length()}.
     */
    long getContentLength();

    /**
     * Return the strong ETag if available (currently not supported) else return
     * the weak ETag calculated from the content length and last modified.
     *
     * @return  The ETag for this resource
     */
    String getETag();

    /**
     * Set the MIME type for this Resource.
     */
    void setMimeType(String mimeType);

    /**
     * Get the MIME type for this Resource.
     */
    String getMimeType();

    /**
     * Obtain an InputStream based on the contents of this resource.
     *
     * @return  An InputStream based on the contents of this resource or
     *          <code>null</code> if the resource does not exist or does not
     *          represent a file
     */
    InputStream getInputStream();
    
    /**
     * The time the file was created. If not available, the result of
     * {@link #getLastModified()} will be returned.
     */
    long getCreation();

    
	/**
	 * Increases the download count by one.
	 */
	public void increaseDownloadCount();

}
