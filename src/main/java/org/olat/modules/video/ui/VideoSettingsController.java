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
package org.olat.modules.video.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.repository.RepositoryEntry;

public class VideoSettingsController extends BasicController {

	private RepositoryEntry entry;

	private VideoMetaDataEditFormController metaDataController;
	private VideoPosterEditController posterEditController;
	private VideoTrackEditController trackEditController;
	private VideoQualityTableFormController qualityEditController;

	private Link metaDataLink, posterEditLink, trackEditLink, qualityConfig;

	private final VelocityContainer mainVC;
	private final SegmentViewComponent segmentView;



	public VideoSettingsController(UserRequest ureq, WindowControl wControl, RepositoryEntry entry ) {
		super(ureq, wControl);

		this.entry = entry;
		mainVC = createVelocityContainer("video_settings");

		segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);

		metaDataLink = LinkFactory.createLink("tab.video.metaDataConfig", mainVC, this);
		segmentView.addSegment(metaDataLink, true);
		posterEditLink = LinkFactory.createLink("tab.video.posterConfig", mainVC, this);
		segmentView.addSegment(posterEditLink, false);
		trackEditLink = LinkFactory.createLink("tab.video.trackConfig", mainVC, this);
		segmentView.addSegment(trackEditLink, false);
		qualityConfig = LinkFactory.createLink("tab.video.qualityConfig", mainVC, this);
		segmentView.addSegment(qualityConfig, false);

		doOpenMetaDataConfig(ureq);

		putInitialPanel(mainVC);

	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source == segmentView) {
			if(event instanceof SegmentViewEvent) {
				SegmentViewEvent sve = (SegmentViewEvent)event;
				String segmentCName = sve.getComponentName();
				Component clickedLink = mainVC.getComponent(segmentCName);
				if (clickedLink == metaDataLink) {
					doOpenMetaDataConfig(ureq);
				} else if (clickedLink == posterEditLink){
					doOpenPosterConfig(ureq);
				} else if (clickedLink == trackEditLink){
					doOpenTrackConfig(ureq);
				} else if (clickedLink == qualityConfig){
					doOpenQualityConfig(ureq);
				}
			}
		}
	}

	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub

	}

	private void doOpenMetaDataConfig(UserRequest ureq) {
		if(metaDataController == null) {
			metaDataController = new VideoMetaDataEditFormController(ureq, getWindowControl(), entry.getOlatResource());
		}
		mainVC.put("segmentCmp", metaDataController.getInitialComponent());
	}

	private void doOpenPosterConfig(UserRequest ureq) {
		if(posterEditController == null) {
			posterEditController = new VideoPosterEditController(ureq, getWindowControl(), entry.getOlatResource());
		}
		mainVC.put("segmentCmp", posterEditController.getInitialComponent());
	}

	private void doOpenTrackConfig(UserRequest ureq) {
		if(trackEditController == null) {
			trackEditController = new VideoTrackEditController(ureq, getWindowControl(), entry.getOlatResource());
		}
		mainVC.put("segmentCmp", trackEditController.getInitialComponent());
	}

	private void doOpenQualityConfig(UserRequest ureq) {
		if(qualityEditController == null) {
			qualityEditController = new VideoQualityTableFormController(ureq, getWindowControl(), entry);
		}
		mainVC.put("segmentCmp", qualityEditController.getInitialComponent());
	}

}
